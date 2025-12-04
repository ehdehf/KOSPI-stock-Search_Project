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

@Service
public class IndexService {

    @Autowired private IndexDAO indexDAO;
    @Autowired private RestTemplate restTemplate;

    private static final String SERVICE_KEY = "bd57b87ea9aa7ba4d2e87197051340c26321a4c486cef4b994b2269766664ccb";
    private static final String API_ENDPOINT = "https://apis.data.go.kr/1160100/service/GetMarketIndexInfoService/getStockMarketIndex";
    private static final int ROWS_PER_PAGE = 500;

    // KOSPI
    private static final String TARGET_INDEX = "코스피";
    private static final String START_DATE = "19800104";

    // KOSDAQ
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

    // ------------------- URL 빌더 (인코딩하지 않음) -------------------
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
    // 서버 시작 시 자동 실행: KOSPI + KOSDAQ 전체 초기 수집 (한 번만)
    // ------------------------------------------------------------
    @PostConstruct
    @Transactional
    public void runInitialFullBackfillIfNeeded() {
        // 코스피
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

        // 코스닥
        try {
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

    // Wrapper: 기존 public method kept for admin API but implementation delegated to internals
    @Transactional
    public void initiateHistoricalDataCollection() {
        // 외부에서 수동 호출시 내부 구현 재사용
        initiateHistoricalDataCollectionInternal();
    }

    // 실제 코스피 전체 초기 수집 로직 (internal, 재사용용)
    @Transactional
    protected void initiateHistoricalDataCollectionInternal() {
        System.out.println("=== KOSPI 전체 데이터 수집 시작 ===");
        try {
            String countUrl = buildApiUrlForTotalCountForIndex(TARGET_INDEX, 1, 1);
            System.out.println("[KOSPI TotalCount URL] " + countUrl);

            String xmlResponse = restTemplate.getForObject(countUrl, String.class);
            List<IndexDataDTO> initialData = parseXml(xmlResponse);

            if (initialData.isEmpty() || initialData.get(0).getTotalCount() == null) {
                System.out.println("⚠ KOSPI totalCount 조회 실패 → 중단");
                return;
            }

            int totalCount = initialData.get(0).getTotalCount();
            int totalPages = (int) Math.ceil((double) totalCount / ROWS_PER_PAGE);

            System.out.println("KOSPI 전체 데이터 수: " + totalCount + ", 총 페이지 수: " + totalPages);

            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

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
                Thread.sleep(200);
            }

            System.out.println("=== KOSPI 전체 데이터 수집 완료 ===");
        } catch (Exception e) {
            System.err.println("KOSPI 치명적 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ---------------- React 차트용 (KOSPI) ----------------
    public List<IndexDataDTO> getKospiTimeSeriesData() {
        return indexDAO.selectKospiHistory();
    }

    // ---------------- 일일 KOSPI 저장 ----------------
    public void saveSingleDayData(String targetDate) {
        try {
            String url = buildApiUrlForIndex(TARGET_INDEX, 1, 1, targetDate, targetDate);
            String xmlResponse = restTemplate.getForObject(url, String.class);
            List<IndexDataDTO> data = parseXml(xmlResponse);

            if (!data.isEmpty() && TARGET_INDEX.equals(data.get(0).getIdxNm())) {
                indexDAO.insertOrUpdateIndexData(data.get(0));
            } else {
                System.out.println("KOSPI " + targetDate + " 데이터 없음 또는 idxNm 불일치");
            }
        } catch (Exception e) {
            System.err.println("KOSPI 일일 저장 오류: " + e.getMessage());
        }
    }

    // ---------------- KOSDAQ 전체 초기 수집 (Admin 또는 자동 호출용) ----------------
    @Transactional
    public void initiateKosdaqHistoricalDataCollection() {
        initiateKosdaqHistoricalDataCollectionInternal();
    }

    @Transactional
    protected void initiateKosdaqHistoricalDataCollectionInternal() {
        System.out.println("=== KOSDAQ 전체 데이터 수집 시작 ===");
        try {
            String countUrl = buildApiUrlForTotalCountForIndex(TARGET_INDEX_KOSDAQ, 1, 1);
            System.out.println("[KOSDAQ TotalCount URL] " + countUrl);

            String xmlResponse = restTemplate.getForObject(countUrl, String.class);
            List<IndexDataDTO> initialData = parseXml(xmlResponse);

            if (initialData.isEmpty() || initialData.get(0).getTotalCount() == null) {
                System.out.println("⚠ KOSDAQ totalCount 조회 실패 → 중단");
                return;
            }

            int totalCount = initialData.get(0).getTotalCount();
            int totalPages = (int) Math.ceil((double) totalCount / ROWS_PER_PAGE);

            System.out.println("KOSDAQ 전체 데이터 수: " + totalCount + ", 총 페이지: " + totalPages);

            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            for (int pageNo = 1; pageNo <= totalPages; pageNo++) {
                String url = buildApiUrlForIndex(TARGET_INDEX_KOSDAQ, pageNo, ROWS_PER_PAGE, START_DATE_KOSDAQ, today);
                String pageXml = restTemplate.getForObject(url, String.class);
                List<IndexDataDTO> pageData = parseXml(pageXml);

                for (IndexDataDTO dto : pageData) {
                    if (TARGET_INDEX_KOSDAQ.equals(dto.getIdxNm())) {
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

    // ---------------- KOSDAQ 일별 저장 ----------------
    public void saveSingleKosdaqDayData(String targetDate) {
        try {
            String url = buildApiUrlForIndex(TARGET_INDEX_KOSDAQ, 1, 1, targetDate, targetDate);
            String xmlResponse = restTemplate.getForObject(url, String.class);
            List<IndexDataDTO> data = parseXml(xmlResponse);

            if (!data.isEmpty() && TARGET_INDEX_KOSDAQ.equals(data.get(0).getIdxNm())) {
                indexDAO.insertOrUpdateKosdaqIndexData(data.get(0));
            } else {
                System.out.println("KOSDAQ " + targetDate + " 데이터 없음 또는 idxNm 불일치");
            }
        } catch (Exception e) {
            System.err.println("KOSDAQ 일일 저장 오류: " + e.getMessage());
        }
    }

    // ---------------- React 차트용 (KOSDAQ) ----------------
    public List<IndexDataDTO> getKosdaqTimeSeriesData() {
        return indexDAO.selectKosdaqHistory();
    }
}
