import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LinkPopularity {
  public static int maxNumberOfRef = 0;
  public static String popularLink = "";
  public static String linkType = "";
  public static String langType = "";
  public static String protocolType = "";
  public static String popularExtUri = "";
  public static String interWikiKey = "";

  public static HashMap<String, String> linkFormatMap = new HashMap<String, String>();

  static {
    linkFormatMap.put("backlinks", "https://en.wikipedia.org/wiki/%s");
    linkFormatMap.put("iwbacklinks", "https://en.wikipedia.org/wiki/");
    linkFormatMap.put("exturlusage", "%s");
    linkFormatMap.put("langbacklinks", "https://%s.wikipedia.org/wiki/%s");
  }

  public static void main(String[] args) throws IOException, JSONException {
    System.out.print("Please enter a wikipedia url to get the most popular link on the page: ");
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String input = br.readLine().split("wikipedia.org/wiki/")[1].split("#")[0];

    System.out.print("Finding the most popular link...");
    popularLink(input, "links", "pllimit");
    popularLink(input, "iwlinks", "iwlimit");
    popularLink(input, "extlinks", "ellimit");
    popularLink(input, "langlinks", "lllimit");
    System.out.println("\nMost popular Link : " + printPopularUrl(popularLink)
        + " count : " + maxNumberOfRef);
  }

  public static String printPopularUrl(String pPopularLink) {
    switch (linkType) {
      case "backlinks": {
        return String.format(linkFormatMap.get(linkType), pPopularLink);
      }
      case "iwbacklinks": {
        return String.format(InterWikiUrlMapper.getInterWikiUrl(interWikiKey),
            pPopularLink);
      }
      case "exturlusage": {
        return String.format(linkFormatMap.get(linkType), popularExtUri);
      }
      case "langbacklinks": {
        return String.format(linkFormatMap.get(linkType), langType, pPopularLink);
      }
      default: {
        return "";
      }
    }
  }

  public static void popularLink(String pTitle, String pProValue,
      String pLimitKey) throws JSONException, IOException {
    String pageUrl = "https://en.wikipedia.org/w/api.php?action=query&titles="
        + pTitle + "&prop=" + pProValue + "&format=json&" + pLimitKey
        + "=max&continue=";
    JSONObject jsonResponse = readJsonFromUrl(pageUrl);
    JSONObject pagesObj = jsonResponse.getJSONObject("query").getJSONObject("pages");
    if (pagesObj.getJSONObject(pagesObj.keys().next().toString()).has(pProValue)) {
      JSONArray linksArray = pagesObj.getJSONObject(pagesObj.keys().next().toString())
          .getJSONArray(pProValue);
      if (pProValue.equals("links")) {
        getAllLinks("plcontinue", pProValue, pageUrl, jsonResponse, pagesObj, linksArray);
        findPopularity(linksArray, "backlinks", "bltitle", "blcontinue", "bllimit");
      }
      if (pProValue.equals("iwlinks")) {
        getAllLinks("iwcontinue", pProValue, pageUrl, jsonResponse, pagesObj, linksArray);
        findPopularity(linksArray, "iwbacklinks", "iwbltitle", "iwblcontinue",
            "iwbllimit");
      }
      if (pProValue.equals("langlinks")) {
        getAllLinks("llcontinue", pProValue, pageUrl, jsonResponse, pagesObj, linksArray);
        findPopularity(linksArray, "langbacklinks", "lbltitle", "lblcontinue",
            "lbllimit");
      }
      if (pProValue.equals("extlinks")) {
        getAllLinks("elcontinue", pProValue, pageUrl, jsonResponse, pagesObj, linksArray);
        findPopularity(linksArray, "exturlusage", "euquery", "euoffset", "eulimit");
      }
    }
  }

  public static void getAllLinks(String pPrefix, String pProValue,
      String pPageUrl, JSONObject pJsonResponse, JSONObject pPagesObj, JSONArray pLinksArray)
      throws JSONException, IOException {
    JSONArray tempArray = null;
    while (pJsonResponse.has("continue")) {
      pJsonResponse = readJsonFromUrl(pPageUrl + "&" + pPrefix + "="
          + pJsonResponse.getJSONObject("continue").get(pPrefix).toString());
      if (pJsonResponse.has("query")) {
        pPagesObj = pJsonResponse.getJSONObject("query").getJSONObject("pages");
        tempArray = pPagesObj.getJSONObject(pPagesObj.keys().next().toString())
            .getJSONArray(pProValue);
      }
      for (int i = 0; i < tempArray.length(); i++) {
        pLinksArray.put(tempArray.getJSONObject(i));
      }
    }
  }

  public static void findPopularity(JSONArray pLinksArray, String pListType,
      String pTitleKey, String pContinueKey, String pLimitKey)
      throws JSONException, IOException {
    for (int i = 0; i < pLinksArray.length(); i++) {
      System.out.print(".");
      int tempMax = 0;
      String url;
      String title;
      String protocol;
      String lang = "en";
      String externalUrl = "";
      String prefix = "";
      JSONObject titleObj = new JSONObject(pLinksArray.get(i).toString());
      int pageCounter = 0;
      if (pListType.equals("backlinks")) {
        title = titleObj.get("title").toString().replaceAll(" ", "%20");
        url = "https://en.wikipedia.org/w/api.php?action=query&list="
            + pListType + "&format=json&" + pTitleKey + "=" + title + "&"
            + pLimitKey + "=max&continue=";

      } else if (pListType.equals("iwbacklinks")) {
        title = titleObj.get("*").toString().replaceAll(" ", "%20");
        prefix = titleObj.get("prefix").toString();
        url = "https://en.wikipedia.org/w/api.php?action=query&list="
            + pListType + "&format=json&" + pTitleKey + "=" + title + "&"
            + pLimitKey + "=max&continue=&iwblprefix=" + prefix;
      } else if (pListType.equals("langbacklinks")) {
        title = titleObj.get("*").toString().replaceAll(" ", "%20");
        lang = titleObj.get("lang").toString();
        url = "https://en.wikipedia.org/w/api.php?action=query&list="
            + pListType + "&format=json&" + pTitleKey + "=" + title + "&"
            + pLimitKey + "=max&continue=&lbllang=" + lang;

      } else {
        if (titleObj.get("*").toString().split("://").length == 2) {
          externalUrl = titleObj.get("*").toString();
          protocol = titleObj.get("*").toString().split("://")[0];

          title = titleObj.get("*").toString().split("://")[1];
        } else if (titleObj.get("*").toString().split("//").length == 2) {
          protocol = "http";
          title = titleObj.get("*").toString().split("//")[1];
        } else {
          protocol = "http";
          title = titleObj.get("*").toString();
        }
        url = "https://en.wikipedia.org/w/api.php?action=query&list="
            + pListType + "&format=json&" + pTitleKey + "=" + title + "&"
            + pLimitKey + "=max&continue=&euprotocol=" + protocol;
      }

      JSONObject jsonBack = readJsonFromUrl(url);
      tempMax += jsonBack.getJSONObject("query").getJSONArray(pListType)
          .length();
      boolean pageCounterTextPrinted = false;
      while (jsonBack.has("continue")) {
        if(pageCounter > 30 && !pageCounterTextPrinted) {
          System.out.println("\nOn a popular link, its references are too many. Please wait.");
          pageCounterTextPrinted = true;
        } else {
          pageCounter++;
        }
        jsonBack = readJsonFromUrl(url + "&" + pContinueKey + "="
            + jsonBack.getJSONObject("continue").get(pContinueKey).toString());
        if (jsonBack.has("query"))
          tempMax += jsonBack.getJSONObject("query").getJSONArray(pListType)
              .length();
      }
      if (maxNumberOfRef < tempMax) {
        maxNumberOfRef = tempMax;
        popularLink = title;
        linkType = pListType;// type of link
        langType = lang;
        popularExtUri = externalUrl;
        interWikiKey = prefix;
      }
    }
  }

  private static String readAll(Reader pReader) throws IOException {
    StringBuilder sb = new StringBuilder();
    int ch;
    while ((ch = pReader.read()) != -1) {
      sb.append((char) ch);
    }
    return sb.toString();
  }

  public static JSONObject readJsonFromUrl(String pUrl) throws IOException,
      JSONException {
    InputStream is = new URL(pUrl).openStream();
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(is,
          Charset.forName("UTF-8")));
      String jsonText = readAll(br);
      JSONObject json = new JSONObject(jsonText);
      return json;
    } finally {
      is.close();
    }
  }
}