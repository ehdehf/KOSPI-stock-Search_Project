package com.boot.service;

import com.boot.dao.IndexDAO;
import com.boot.dto.IndexDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.*;
import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
// 🌟 CacheEvictService 주입 (collectDataRangeAndSave에서 사용)
import com.boot.service.CacheEvictService; 

@Service
public class IndexService {

    @Autowired private IndexDAO indexDAO;
    @Autowired private RestTemplate restTemplate; 
    
    // 🌟 CacheEvictService 주입
    @Autowired private CacheEvictService cacheEvictService;

    private static final String SERVICE_KEY = "bd57b87ea9aa7ba4d2e87197051340c26321a4c486cef4b994b2269766664ccb";
    private static final String API_ENDPOINT = "https://apis.data.go.kr/1160100/service/GetMarketIndexInfoService/getStockMarketIndex";
    private static final int ROWS_PER_PAGE = 500;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    // Redis 캐시 상수 (KOSPI)
    private static final String KOSPI_CACHE_NAME = "kospiHistoryCache";
    private static final String KOSPI_CACHE_KEY = "'kospi_all'";
    
    // Redis 캐시 상수 (KOSDAQ)
    private static final String KOSDAQ_CACHE_NAME = "kosdaqHistoryCache";
    private static final String KOSDAQ_CACHE_KEY = "'kosdaq_all'";

    // KOSPI 상수
    private static final String TARGET_INDEX = "코스피";
    private static final String START_DATE = "19800104"; // KOSPI 시작일

    // KOSDAQ 상수
    private static final String TARGET_INDEX_KOSDAQ = "코스닥";
    private static final String START_DATE_KOSDAQ = "19960701"; // KOSDAQ 시작일

    // ------------------- XML 파싱 유틸 -------------------
    private String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                return node.getTextContent();
            }
        }
        return null;
    }

    private List<IndexDataDTO> parseXml(String xmlData) {
        List<IndexDataDTO> resultList = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlData.getBytes("UTF-8")));
            doc.getDocumentElement().normalize();

            NodeList totalCountNode = doc.getElementsByTagName("totalCount");
            int totalCount = (totalCountNode.getLength() > 0)
                    ? Integer.parseInt(totalCountNode.item(0).getTextContent())
                    : 0;

            NodeList itemList = doc.getElementsByTagName("item");

            for (int i = 0; i < itemList.getLength(); i++) {
                Node itemNode = itemList.item(i);
                if (itemNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) itemNode;

                    IndexDataDTO dto = new IndexDataDTO();
                    dto.setIdxNm(getTagValue("idxNm", element));
                    dto.setBasDt(getTagValue("basDt", element));

                    try { dto.setClpr(Double.parseDouble(getTagValue("clpr", element))); } catch (Exception e) { dto.setClpr(null); }
                    try { dto.setVs(Double.parseDouble(getTagValue("vs", element))); } catch (Exception e) { dto.setVs(null); }
                    try { dto.setFltRt(Double.parseDouble(getTagValue("fltRt", element))); } catch (Exception e) { dto.setFltRt(null); }
                    try { dto.setMkp(Double.parseDouble(getTagValue("mkp", element))); } catch (Exception e) { dto.setMkp(null); }
                    try { dto.setHipr(Double.parseDouble(getTagValue("hipr", element))); } catch (Exception e) { dto.setHipr(null); }
                    try { dto.setLopr(Double.parseDouble(getTagValue("lopr", element))); } catch (Exception e) { dto.setLopr(null); }
                    try { dto.setTrqu(Long.parseLong(getTagValue("trqu", element))); } catch (Exception e) { dto.setTrqu(null); }
                    try { dto.setTrPrc(Long.parseLong(getTagValue("trPrc", element))); } catch (Exception e) { dto.setTrPrc(null); }
                    try { dto.setLstgMrktTotAmt(Long.parseLong(getTagValue("lstgMrktTotAmt", element))); } catch (Exception e) { dto.setLstgMrktTotAmt(null); }

                    if (i == 0) dto.setTotalCount(totalCount);

                    resultList.add(dto);
                }
            }
        } catch (Exception e) {
            System.err.println("XML 파싱 오류: " + e.getMessage());
        }
        return resultList;
    }

    // ------------------- URL 빌더 -------------------
    
    private String buildApiUrlForIndex(String idxNm, int pageNo, int numOfRows, String beginDt, String endDt) {
        return UriComponentsBuilder.fromUriString(API_ENDPOINT)
                .queryParam("serviceKey", SERVICE_KEY)
                .queryParam("resultType", "xml")
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", numOfRows)
                .queryParam("idxNm", idxNm)
                .queryParam("beginBasDt", beginDt)
                .queryParam("endBasDt", endDt)
                .build(false)
                .toUriString();
    }

    // ------------------------------------------------------------
    // 🌟 서버 시작 시 자동 실행: 누락분 업데이트 로직 (캐시 삭제 로직 제거)
    // ------------------------------------------------------------
    @PostConstruct
    public void runInitialFullBackfillIfNeeded() {
        // 🔴 여기서 직접 캐시를 지우는 로직을 제거합니다. CacheInitializerService가 담당합니다.
        System.out.println("AUTO INIT: KOSPI/KOSDAQ 데이터 유효성 검사 및 누락분 수집 시작");
        
        // KOSPI 데이터 유효성 검사 및 업데이트
        try {
            updateMissingIndexData(
                TARGET_INDEX, 
                START_DATE, 
                indexDAO.countIndexData(TARGET_INDEX),
                indexDAO.selectLatestBasDt(TARGET_INDEX) 
            );
        } catch (Exception e) {
            System.err.println("AUTO INIT: KOSPI 초기 수집 중 오류: " + e.getMessage());
            e.printStackTrace();
        }

        // KOSDAQ 데이터 유효성 검사 및 업데이트
        try {
            updateMissingIndexData(
                TARGET_INDEX_KOSDAQ, 
                START_DATE_KOSDAQ, 
                indexDAO.countKosdaqIndexData(TARGET_INDEX_KOSDAQ), 
                indexDAO.selectLatestKosdaqBasDt(TARGET_INDEX_KOSDAQ) 
            );
        } catch (Exception e) {
            System.err.println("AUTO INIT: KOSDAQ 초기 수집 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("AUTO INIT: 누락분 업데이트 검사 완료");
    }

    // ==========================================================
    // KOSPI/KOSDAQ 수동 전체 수집 API (누락분 업데이트 로직 호출로 변경)
    // ==========================================================
    
    @Transactional
    public void initiateHistoricalDataCollection() {
         updateMissingIndexData(
            TARGET_INDEX, 
            START_DATE, 
            indexDAO.countIndexData(TARGET_INDEX), 
            indexDAO.selectLatestBasDt(TARGET_INDEX)
         );
    }
    
    @Transactional
    public void initiateKosdaqHistoricalDataCollection() {
         updateMissingIndexData(
            TARGET_INDEX_KOSDAQ, 
            START_DATE_KOSDAQ, 
            indexDAO.countKosdaqIndexData(TARGET_INDEX_KOSDAQ), 
            indexDAO.selectLatestKosdaqBasDt(TARGET_INDEX_KOSDAQ)
         );
    }


    // ==========================================================
    // 🌟 핵심 로직: DB의 가장 최근 날짜 이후의 누락분을 수집 (공통 사용)
    // ==========================================================
    @Transactional
    protected void updateMissingIndexData(String idxNm, String initialStartDate, int dbCount, String latestDtInDB) {
        String startDateToFetch = initialStartDate;
        
        // DB에 데이터가 있다면, 가장 최근 날짜의 다음 날부터 시작
        if (dbCount > 10 && latestDtInDB != null && !latestDtInDB.isEmpty()) {
            try {
                LocalDate latestDate = LocalDate.parse(latestDtInDB, DATE_FORMATTER);
                startDateToFetch = latestDate.plusDays(1).format(DATE_FORMATTER);
                
                System.out.println(idxNm + " 데이터 발견. 업데이트 시작 날짜: " + startDateToFetch);
                
            } catch (Exception e) {
                System.err.println(idxNm + " 최근 날짜 파싱 오류. 초기 시작일(" + initialStartDate + ")로 대체: " + e.getMessage());
                startDateToFetch = initialStartDate;
            }
        } else {
            System.out.println(idxNm + " DB 데이터 부족. 초기 수집 시작 날짜: " + initialStartDate);
        }
        
        // 오늘 날짜
        String today = LocalDate.now().format(DATE_FORMATTER);

        // 이미 최신 날짜 이후라면 업데이트 불필요
        if (startDateToFetch.compareTo(today) > 0) {
            System.out.println(idxNm + " 최신 데이터입니다. 업데이트 불필요.");
            return;
        }

        // 2. API 호출 (시작일 ~ 오늘)
        collectDataRangeAndSave(idxNm, ROWS_PER_PAGE, startDateToFetch, today);
    }
    
    // ==========================================================
    // 공통 수집 및 저장 유틸리티
    // ==========================================================
    
    @Transactional
    protected void collectDataRangeAndSave(String idxNm, int rowsPerPage, String beginDt, String endDt) {
         System.out.println("=== " + idxNm + " 데이터 수집 시작: " + beginDt + " ~ " + endDt + " ===");
         int totalCount = 0;
         int totalPages = 0;
         
         try {
             // 1. 전체 건수를 가져오기 위한 초기 API 호출 (범위 기반)
             String countUrl = buildApiUrlForIndex(idxNm, 1, 1, beginDt, endDt);
             String xmlResponse = restTemplate.getForObject(countUrl, String.class);
             List<IndexDataDTO> initialData = parseXml(xmlResponse);

             if (initialData.isEmpty() || initialData.get(0).getTotalCount() == null) {
                 System.err.println("⚠ " + idxNm + " totalCount 조회 실패 또는 데이터 없음.");
                 return;
             }

             totalCount = initialData.get(0).getTotalCount();
             totalPages = (int) Math.ceil((double) totalCount / rowsPerPage);

             if (totalCount == 0) {
                 System.out.println(idxNm + " 수집 기간 내 신규 데이터 없음.");
                 return;
             }
             
             // 2. 전체 페이지 순회 및 DB 저장
             for (int pageNo = 1; pageNo <= totalPages; pageNo++) {
                 String url = buildApiUrlForIndex(idxNm, pageNo, rowsPerPage, beginDt, endDt);
                 String pageXml = restTemplate.getForObject(url, String.class);
                 List<IndexDataDTO> pageData = parseXml(pageXml);

                 for (IndexDataDTO dto : pageData) {
                     if (idxNm.equals(dto.getIdxNm())) {
                         if (idxNm.equals(TARGET_INDEX)) {
                             indexDAO.insertOrUpdateIndexData(dto); // KOSPI 테이블 저장
                         } else if (idxNm.equals(TARGET_INDEX_KOSDAQ)) {
                             indexDAO.insertOrUpdateKosdaqIndexData(dto); // KOSDAQ 테이블 저장
                         }
                     }
                 }
                 System.out.println(idxNm + " 페이지 " + pageNo + " 완료 (" + pageData.size() + "건)");
                 Thread.sleep(200); // API 부하 방지
             }

             System.out.println("=== " + idxNm + " 데이터 수집 완료 (총 " + totalCount + "건) ===");
             
             // 🔴 이 위치에서 캐시 삭제 로직을 제거합니다. 
             // 캐시 삭제는 CacheInitializerService가 담당합니다.

         } catch (Exception e) {
             System.err.println(idxNm + " 데이터 수집 중 치명적 오류: " + e.getMessage());
             e.printStackTrace();
         }
    }


    // ==========================================================
    // 캐시 및 데이터 조회 메서드 (Cacheable 유지)
    // ==========================================================

    // KOSPI 조회
    @Cacheable(value = KOSPI_CACHE_NAME, key = KOSPI_CACHE_KEY)
    public List<IndexDataDTO> getKospiTimeSeriesData() {
        System.out.println("DEBUG: DB에서 KOSPI 히스토리 조회 중 (Cache Miss)...");
        return indexDAO.selectKospiHistory();
    }
    
    // KOSDAQ 조회
    @Cacheable(value = KOSDAQ_CACHE_NAME, key = KOSDAQ_CACHE_KEY)
    public List<IndexDataDTO> getKosdaqTimeSeriesData() {
        System.out.println("DEBUG: DB에서 KOSDAQ 히스토리 조회 중 (Cache Miss)...");
        return indexDAO.selectKosdaqHistory();
    }
    
    // ---------------- KOSPI 일일 저장 (수동 호출용) ----------------
    @Transactional
    public void saveSingleDayData(String targetDate) {
        collectDataRangeAndSave(TARGET_INDEX, 1, targetDate, targetDate); 
    }
    
    // ---------------- KOSDAQ 일별 저장 (수동 호출용) ----------------
    @Transactional
    public void saveSingleKosdaqDayData(String targetDate) {
        collectDataRangeAndSave(TARGET_INDEX_KOSDAQ, 1, targetDate, targetDate); 
    }
}