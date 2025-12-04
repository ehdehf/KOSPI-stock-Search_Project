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
import org.springframework.cache.annotation.CacheEvict;

@Service
public class IndexService {

    @Autowired private IndexDAO indexDAO;
    @Autowired private RestTemplate restTemplate; 

    private static final String SERVICE_KEY = "bd57b87ea9aa7ba4d2e87197051340c26321a4c486cef4b994b2269766664ccb";
    private static final String API_ENDPOINT = "https://apis.data.go.kr/1160100/service/GetMarketIndexInfoService/getStockMarketIndex";
    private static final int ROWS_PER_PAGE = 500;
    
    // Redis 캐시 상수 (KOSPI)
    private static final String KOSPI_CACHE_NAME = "kospiHistoryCache";
    private static final String KOSPI_CACHE_KEY = "'kospi_all'";
    
    // Redis 캐시 상수 (KOSDAQ)
    private static final String KOSDAQ_CACHE_NAME = "kosdaqHistoryCache";
    private static final String KOSDAQ_CACHE_KEY = "'kosdaq_all'";

    // KOSPI 상수
    private static final String TARGET_INDEX = "코스피";
    private static final String START_DATE = "19800104";

    // KOSDAQ 상수
    private static final String TARGET_INDEX_KOSDAQ = "코스닥";
    private static final String START_DATE_KOSDAQ = "19960701";

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

                    // 숫자 필드는 파싱 실패 시 DB에 NULL을 넣기 위해 안전하게 처리
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

    // ------------------- URL 빌더 (build(false) 사용) -------------------
    
    private String buildApiUrlForTotalCountForIndex(String idxNm, int pageNo, int numOfRows) {
        return UriComponentsBuilder.fromUriString(API_ENDPOINT)
                .queryParam("serviceKey", SERVICE_KEY)
                .queryParam("resultType", "xml")
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", numOfRows)
                .queryParam("idxNm", idxNm)
                .build(false) 
                .toUriString();
    }

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
    // 서버 시작 시 자동 실행: KOSPI + KOSDAQ 전체 초기 수집
    // ------------------------------------------------------------
    @PostConstruct
    public void runInitialFullBackfillIfNeeded() {
        // KOSPI 초기 데이터 수집 (STOCK_INDEX_DATA 테이블)
        try {
            if (indexDAO.countIndexData(TARGET_INDEX) <= 10) {
                System.out.println("AUTO INIT: KOSPI 전체 초기 수집 시작");
                initiateHistoricalDataCollectionInternal();
                System.out.println("AUTO INIT: KOSPI 전체 초기 수집 완료");
            } else {
                System.out.println("AUTO INIT: KOSPI 데이터 충분 → 스킵");
            }
        } catch (Exception e) {
            System.err.println("AUTO INIT: KOSPI 초기 수집 중 오류: " + e.getMessage());
            e.printStackTrace();
        }

        // KOSDAQ 초기 데이터 수집 (STOCK_INDEX_DATA_KOSDAQ 테이블)
        try {
            // IndexDAO에 KOSDAQ 테이블 카운트 메소드가 있다고 가정 (countKosdaqIndexData)
            if (indexDAO.countKosdaqIndexData(TARGET_INDEX_KOSDAQ) <= 10) { 
                System.out.println("AUTO INIT: KOSDAQ 전체 초기 수집 시작");
                initiateKosdaqHistoricalDataCollectionInternal();
                System.out.println("AUTO INIT: KOSDAQ 전체 초기 수집 완료");
            } else {
                System.out.println("AUTO INIT: KOSDAQ 데이터 충분 → 스킵");
            }
        } catch (Exception e) {
            System.err.println("AUTO INIT: KOSDAQ 초기 수집 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==========================================================
    // KOSPI 전용 로직 (STOCK_INDEX_DATA 테이블 사용)
    // ==========================================================

    @Transactional
    public void initiateHistoricalDataCollection() {
        initiateHistoricalDataCollectionInternal();
    }

    @Transactional
    protected void initiateHistoricalDataCollectionInternal() {
        System.out.println("=== KOSPI 전체 데이터 수집 시작 ===");
        try {
            // 1. totalCount 확인
            String countUrl = buildApiUrlForTotalCountForIndex(TARGET_INDEX, 1, 1);
            String xmlResponse = restTemplate.getForObject(countUrl, String.class);
            List<IndexDataDTO> initialData = parseXml(xmlResponse);

            if (initialData.isEmpty() || initialData.get(0).getTotalCount() == null) {
                System.err.println("⚠ KOSPI totalCount 조회 실패 → 중단.");
                return;
            }

            int totalCount = initialData.get(0).getTotalCount();
            int totalPages = (int) Math.ceil((double) totalCount / ROWS_PER_PAGE);
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            // 2. 전체 페이지 순회 및 DB 저장
            for (int pageNo = 1; pageNo <= totalPages; pageNo++) {
                String url = buildApiUrlForIndex(TARGET_INDEX, pageNo, ROWS_PER_PAGE, START_DATE, today);
                String pageXml = restTemplate.getForObject(url, String.class);
                List<IndexDataDTO> pageData = parseXml(pageXml);

                for (IndexDataDTO dto : pageData) {
                    if (TARGET_INDEX.equals(dto.getIdxNm())) {
                        indexDAO.insertOrUpdateIndexData(dto);
                    }
                }
                System.out.println("KOSPI 페이지 " + pageNo + " 완료 (" + pageData.size() + "건)");
                Thread.sleep(200); // API 부하 방지
            }

            System.out.println("=== KOSPI 전체 데이터 수집 완료 ===");
        } catch (Exception e) {
            System.err.println("KOSPI 치명적 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ---------------- React 차트용 (KOSPI) ----------------
    // 🌟 Redis 캐싱 처리 적용: 캐시가 있으면 DB 조회 없이 즉시 반환
    @Cacheable(value = KOSPI_CACHE_NAME, key = KOSPI_CACHE_KEY)
    public List<IndexDataDTO> getKospiTimeSeriesData() {
        System.out.println("DEBUG: DB에서 KOSPI 히스토리 조회 중 (Cache Miss)...");
        return indexDAO.selectKospiHistory();
    }

    // ---------------- 일일 KOSPI 저장 ----------------
    // 🌟 Redis 캐싱 처리 적용: DB 업데이트 후 캐시 삭제
    @CacheEvict(value = KOSPI_CACHE_NAME, key = KOSPI_CACHE_KEY)
    @Transactional 
    public void saveSingleDayData(String targetDate) {
        try {
            String url = buildApiUrlForIndex(TARGET_INDEX, 1, 1, targetDate, targetDate);
            String xmlResponse = restTemplate.getForObject(url, String.class);
            List<IndexDataDTO> data = parseXml(xmlResponse);

            if (!data.isEmpty() && TARGET_INDEX.equals(data.get(0).getIdxNm())) {
                indexDAO.insertOrUpdateIndexData(data.get(0));
                System.out.println("KOSPI 데이터 DB 저장 완료 및 캐시 [" + KOSPI_CACHE_KEY + "] 삭제 완료.");
            } else {
                System.out.println("KOSPI " + targetDate + " 데이터 없음 또는 idxNm 불일치");
            }
        } catch (Exception e) {
            System.err.println("KOSPI 일일 저장 오류: " + e.getMessage());
        }
    }


    // ==========================================================
    // KOSDAQ 전용 로직 (STOCK_INDEX_DATA_KOSDAQ 테이블 사용)
    // ==========================================================

    @Transactional
    public void initiateKosdaqHistoricalDataCollection() {
        initiateKosdaqHistoricalDataCollectionInternal();
    }

    @Transactional
    protected void initiateKosdaqHistoricalDataCollectionInternal() {
        System.out.println("=== KOSDAQ 전체 데이터 수집 시작 ===");
        try {
            // 1. totalCount 확인
            String countUrl = buildApiUrlForTotalCountForIndex(TARGET_INDEX_KOSDAQ, 1, 1);
            String xmlResponse = restTemplate.getForObject(countUrl, String.class);
            List<IndexDataDTO> initialData = parseXml(xmlResponse);

            if (initialData.isEmpty() || initialData.get(0).getTotalCount() == null) {
                System.err.println("⚠ KOSDAQ totalCount 조회 실패 → 중단.");
                return;
            }

            int totalCount = initialData.get(0).getTotalCount();
            int totalPages = (int) Math.ceil((double) totalCount / ROWS_PER_PAGE);
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            // 2. 전체 페이지 순회 및 DB 저장
            for (int pageNo = 1; pageNo <= totalPages; pageNo++) {
                String url = buildApiUrlForIndex(TARGET_INDEX_KOSDAQ, pageNo, ROWS_PER_PAGE, START_DATE_KOSDAQ, today);
                String pageXml = restTemplate.getForObject(url, String.class);
                List<IndexDataDTO> pageData = parseXml(pageXml);

                for (IndexDataDTO dto : pageData) {
                    if (TARGET_INDEX_KOSDAQ.equals(dto.getIdxNm())) {
                        // KOSDAQ 테이블에 저장하는 전용 DAO 메소드 호출 가정
                        indexDAO.insertOrUpdateKosdaqIndexData(dto); 
                    }
                }
                System.out.println("KOSDAQ 페이지 " + pageNo + " 완료 (" + pageData.size() + "건)");
                Thread.sleep(200);
            }

            System.out.println("=== KOSDAQ 전체 데이터 수집 완료 ===");
        } catch (Exception e) {
            System.err.println("KOSDAQ 치명적 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ---------------- React 차트용 (KOSDAQ) ----------------
    // 🌟 Redis 캐싱 처리 적용: 캐시가 있으면 DB 조회 없이 즉시 반환
    @Cacheable(value = KOSDAQ_CACHE_NAME, key = KOSDAQ_CACHE_KEY)
    public List<IndexDataDTO> getKosdaqTimeSeriesData() {
        // KOSDAQ 테이블에서 조회하는 전용 DAO 메소드 호출 가정
        System.out.println("DEBUG: DB에서 KOSDAQ 히스토리 조회 중 (Cache Miss)...");
        return indexDAO.selectKosdaqHistory(); 
    }
    
    // ---------------- KOSDAQ 일별 저장 ----------------
    // 🌟 Redis 캐싱 처리 적용: DB 업데이트 후 KOSDAQ 캐시 삭제
    @CacheEvict(value = KOSDAQ_CACHE_NAME, key = KOSDAQ_CACHE_KEY)
    @Transactional
    public void saveSingleKosdaqDayData(String targetDate) {
        try {
            String url = buildApiUrlForIndex(TARGET_INDEX_KOSDAQ, 1, 1, targetDate, targetDate);
            String xmlResponse = restTemplate.getForObject(url, String.class);
            List<IndexDataDTO> data = parseXml(xmlResponse);

            if (!data.isEmpty() && TARGET_INDEX_KOSDAQ.equals(data.get(0).getIdxNm())) {
                // KOSDAQ 테이블에 저장하는 전용 DAO 메소드 호출 가정
                indexDAO.insertOrUpdateKosdaqIndexData(data.get(0));
                System.out.println("KOSDAQ 데이터 DB 저장 완료 및 캐시 [" + KOSDAQ_CACHE_KEY + "] 삭제 완료.");
            } else {
                System.out.println("KOSDAQ " + targetDate + " 데이터 없음 또는 idxNm 불일치");
            }
        } catch (Exception e) {
            System.err.println("KOSDAQ 일일 저장 오류: " + e.getMessage());
        }
    }
}