package com.bestgo.admanager.servlet;

import com.bestgo.admanager.utils.NumberUtil;
import com.bestgo.admanager.utils.Utils;
import com.bestgo.common.database.services.DB;
import com.bestgo.common.database.utils.JSObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@WebServlet(name = "temp_query2", urlPatterns = {"/temp_query2"}, asyncSupported = true)
public class TempQuery2 extends BaseHttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.doPost(request, response);
        if (!Utils.isAdmin(request, response)) return;

        JsonObject json = new JsonObject();

        String tag = request.getParameter("tag");
        String startTime = request.getParameter("startTime");
        String endTime = request.getParameter("endTime");
        String emptyCampaign = request.getParameter("emptyCampaign");
        String isSummary = request.getParameter("summary");
        String sorterId = request.getParameter("sorterId");
        String admobCheck = request.getParameter("admobCheck");
        String countryCheck = request.getParameter("countryCheck");
        String plusAdmobCheck = request.getParameter("plusAdmobCheck");

        if (isSummary != null) {
            try {
                List<JSObject> tags = DB.scan("web_tag")
                        .select("id", "tag_name").orderByAsc("tag_name").execute();
                JsonArray arr = new JsonArray();
                if (plusAdmobCheck != null && "true".equals(plusAdmobCheck)) {
                    for (int i = 0; i < tags.size(); i++) {
                        long id = tags.get(i).get("id");
                        String tagName = tags.get(i).get("tag_name");
                        JsonObject admob = fetchOneAppData(id, startTime, endTime, emptyCampaign, 0, true, false);
                        JsonObject facebook = fetchOneAppData(id, startTime, endTime, emptyCampaign, 0, false, false);
                        double total_spend = admob.get("total_spend").getAsDouble() + facebook.get("total_spend").getAsDouble();
                        double total_installed = admob.get("total_installed").getAsDouble() + facebook.get("total_installed").getAsDouble();
                        double total_impressions = admob.get("total_impressions").getAsDouble() + facebook.get("total_impressions").getAsDouble();
                        double total_click = admob.get("total_click").getAsDouble() + facebook.get("total_click").getAsDouble();
                        double total_ctr = total_impressions > 0 ? total_click / total_impressions : 0;
                        double total_cpa = total_installed > 0 ? total_spend / total_installed : 0;
                        double total_cvr = total_click > 0 ? total_installed / total_click : 0;
                        admob.addProperty("total_spend", total_spend);
                        admob.addProperty("total_installed", total_installed);
                        admob.addProperty("total_impressions", total_impressions);
                        admob.addProperty("total_click", total_click);
                        admob.addProperty("total_ctr", NumberUtil.trimDouble(total_ctr,4));
                        admob.addProperty("total_cpa", NumberUtil.trimDouble(total_cpa,4));
                        admob.addProperty("total_cvr", NumberUtil.trimDouble(total_cvr,4));
                        admob.addProperty("name", tagName);
                        arr.add(admob);
                    }
                } else {
                    for (int i = 0; i < tags.size(); i++) {
                        long id = tags.get(i).get("id");
                        String tagName = tags.get(i).get("tag_name");
                        JsonObject jsonObject = fetchOneAppData(id, startTime, endTime, emptyCampaign, 0, "true".equals(admobCheck), false);
                        jsonObject.addProperty("name", tagName);
                        arr.add(jsonObject);
                    }
                }
                json.add("data", arr);

                json.addProperty("ret", 1);
                json.addProperty("message", "执行成功");
            } catch (Exception ex) {
                json.addProperty("ret", 0);
                json.addProperty("message", ex.getMessage());
                Logger logger = Logger.getRootLogger();
                logger.error(ex.getMessage(), ex);
            }
        } else {
            try {
                int sorter = 0;
                if (sorterId != null) {
                    sorter = NumberUtil.parseInt(sorterId, 0);
                }
                JSObject tagObject = DB.simpleScan("web_tag")
                        .select("id", "tag_name")
                        .where(DB.filter().whereEqualTo("tag_name", tag)).execute();
                if (tagObject.hasObjectData()) {
                    Long id = tagObject.get("id");
                    JsonObject jsonObject = null;
                    if (plusAdmobCheck != null && "true".equals(plusAdmobCheck)) {
                        JsonObject admob = fetchOneAppData(id, startTime, endTime, emptyCampaign, sorter, true, "true".equals(countryCheck));
                        JsonObject facebook = fetchOneAppData(id, startTime, endTime, emptyCampaign, sorter, false, "true".equals(countryCheck));
                        double total_spend = admob.get("total_spend").getAsDouble() + facebook.get("total_spend").getAsDouble();
                        double total_installed = admob.get("total_installed").getAsDouble() + facebook.get("total_installed").getAsDouble();
                        double total_impressions = admob.get("total_impressions").getAsDouble() + facebook.get("total_impressions").getAsDouble();
                        double total_click = admob.get("total_click").getAsDouble() + facebook.get("total_click").getAsDouble();
                        double total_ctr = total_impressions > 0 ? total_click / total_impressions : 0;
                        double total_cpa = total_installed > 0 ? total_spend / total_installed : 0;
                        double total_cvr = total_click > 0 ? total_installed / total_click : 0;
                        admob.addProperty("total_spend", total_spend);
                        admob.addProperty("total_installed", total_installed);
                        admob.addProperty("total_impressions", total_impressions);
                        admob.addProperty("total_click", total_click);
                        admob.addProperty("total_ctr", NumberUtil.trimDouble(total_ctr,4));
                        admob.addProperty("total_cpa", NumberUtil.trimDouble(total_cpa,4));
                        admob.addProperty("total_cvr", NumberUtil.trimDouble(total_cvr,4));
                        JsonArray array = admob.getAsJsonArray("array");
                        JsonArray array1 = facebook.getAsJsonArray("array");
                        for (int i = 0; i < array1.size(); i++) {
                            array.add(array1.get(i));
                        }
                        jsonObject = admob;
                    } else {
                        jsonObject = fetchOneAppData(id, startTime, endTime, emptyCampaign, sorter, "true".equals(admobCheck), "true".equals(countryCheck));
                    }
                    if ("true".equals(countryCheck)) {
                        JsonArray array = jsonObject.getAsJsonArray("array");
                        HashMap<String, CountryRecord> dataSets = new HashMap<>();
                        for (int i = 0; i < array.size(); i++) {
                            JsonObject one = array.get(i).getAsJsonObject();
                            String countryName = "";
                            if (one.get("country_name").isJsonNull()) {
                                countryName = one.get("country_code").getAsString();
                            } else {
                                countryName = one.get("country_name").getAsString();
                            }
                            CountryRecord record = dataSets.get(countryName);
                            if (record == null) {
                                record = new CountryRecord();
                                dataSets.put(countryName, record);
                            }
                            record.impressions += one.get("impressions").getAsDouble();
                            record.installed += one.get("installed").getAsDouble();
                            record.click += one.get("click").getAsDouble();
                            record.spend += one.get("spend").getAsDouble();
                            record.ctr = record.impressions > 0 ? record.click / record.impressions : 0;
                            record.cpa = record.installed > 0 ? record.spend / record.installed : 0;
                            record.cvr = record.click > 0 ? record.installed / record.click : 0;
                        }
                        JsonArray newArr = new JsonArray();
                        for (String key : dataSets.keySet()) {
                            JsonObject one = new JsonObject();
                            one.addProperty("country_name", key);
                            one.addProperty("impressions", dataSets.get(key).impressions);
                            one.addProperty("installed", dataSets.get(key).installed);
                            one.addProperty("click", dataSets.get(key).click);
                            one.addProperty("spend", NumberUtil.trimDouble(dataSets.get(key).spend,4));
                            one.addProperty("ctr", NumberUtil.trimDouble(dataSets.get(key).ctr,4));
                            one.addProperty("cpa", NumberUtil.trimDouble(dataSets.get(key).cpa,4));
                            one.addProperty("cvr", NumberUtil.trimDouble(dataSets.get(key).cvr,4));
                            newArr.add(one);
                        }
                        jsonObject.add("array", newArr);
                    }
                    json.add("data", jsonObject);

                    json.addProperty("ret", 1);
                    json.addProperty("message", "执行成功");
                } else {
                    json.addProperty("ret", 0);
                    json.addProperty("message", "标签不存在");
                }
            } catch (Exception ex) {
                json.addProperty("ret", 0);
                json.addProperty("message", ex.getMessage());
                Logger logger = Logger.getRootLogger();
                logger.error(ex.getMessage(), ex);
            }
        }

        response.getWriter().write(json.toString());
    }

    class CountryRecord {
        public double impressions;
        public double installed;
        public double click;
        public double spend;
        public double ctr;
        public double cpa;
        public double cvr;
    }

    private JsonObject fetchOneAppData(long tagId, String startTime, String endTime, String emptyCampaign,
                                       int sorterId, boolean admobCheck, boolean countryCheck) throws Exception {
        String relationTable = "web_ad_campaign_tag_rel";
        String webAdCampaignTable = "web_ad_campaigns";
        String webAdCampaignHistoryTable = "web_ad_campaigns_history";
        if (countryCheck) {
            webAdCampaignHistoryTable = "web_ad_campaigns_country_history";
        }
        if (admobCheck) {
            relationTable = "web_ad_campaign_tag_admob_rel";
            webAdCampaignTable = "web_ad_campaigns_admob";
            webAdCampaignHistoryTable = "web_ad_campaigns_history_admob";
            if (countryCheck) {
                webAdCampaignHistoryTable = "web_ad_campaigns_country_history_admob_temp2";
            }
        }

        HashMap<String ,String> countryMap = Utils.getCountryCodeNameMap();

        List<JSObject> list = DB.scan(relationTable).select("campaign_id")
                .where(DB.filter().whereEqualTo("tag_id", tagId)).execute();
        String campaignIds = "";
        for (int i = 0; i < list.size(); i++) {
            campaignIds += (list.get(i).get("campaign_id") + ",");
        }
        if (campaignIds.length() > 0) {
            campaignIds = campaignIds.substring(0, campaignIds.length() - 1);
        }

        String orderStr = "";
        if (sorterId > 0) {
            switch (sorterId) {
                case 1:
                case 1001:
                    orderStr = "order by create_time ";
                    break;
                case 2:
                case 1002:
                    orderStr = "order by status ";
                    break;
                case 3:
                case 1003:
                    orderStr = "order by budget ";
                    break;
                case 4:
                case 1004:
                    orderStr = "order by bidding ";
                    break;
                case 5:
                case 1005:
                    orderStr = "order by spend ";
                    break;
                case 6:
                case 1006:
                    orderStr = "order by installed ";
                    break;
                case 7:
                case 1007:
                    orderStr = "order by click ";
                    break;
                case 8:
                case 1008:
                    orderStr = "order by cpa ";
                    break;
                case 9:
                case 1009:
                    orderStr = "order by ctr ";
                    break;
                case 10:
                case 1010:
                    orderStr = "order by cvr ";
                    break;
            }
            if (sorterId > 1000) {
                orderStr += " desc";
            } else {
                orderStr += " asc";
            }
        }

        if (!campaignIds.isEmpty()) {
            String sql = "select campaign_id, account_id, campaign_name, status, create_time, budget, bidding, spend, installed, impressions, click" +
                    ", (case when impressions > 0 then click/impressions else 0 end) as ctr" +
                    ", (case when installed > 0 then spend/installed else 0 end) as cpa" +
                    ", (case when click > 0 then installed/click else 0 end) as cvr" +
                    " from (" +
                    "select ch.campaign_id, account_id, campaign_name,c.status, create_time, c.budget, c.bidding, sum(ch.total_spend) as spend, " +
                    "sum(ch.total_installed) as installed, sum(ch.total_impressions) as impressions " +
                    ",sum(ch.total_click) as click from " + webAdCampaignTable + " c, " + webAdCampaignHistoryTable + " ch " +
                    "where c.campaign_id=ch.campaign_id\n" +
                    (admobCheck ? " " : "and date between '" + startTime + "' and '" + endTime + "' " )+
                    "and c.campaign_id in (" + campaignIds + ")" +
                    "group by ch.campaign_id) a " + orderStr;
            if (countryCheck) {
                sql = "select campaign_id, country_code, account_id, campaign_name, status, create_time, budget, bidding, spend, installed, impressions, click" +
                        ", (case when impressions > 0 then click/impressions else 0 end) as ctr" +
                        ", (case when installed > 0 then spend/installed else 0 end) as cpa" +
                        ", (case when click > 0 then installed/click else 0 end) as cvr" +
                        " from (" +
                        "select ch.campaign_id, country_code, account_id, campaign_name,c.status, create_time, c.budget, c.bidding, sum(ch.total_spend) as spend, " +
                        "sum(ch.total_installed) as installed, sum(ch.total_impressions) as impressions " +
                        ",sum(ch.total_click) as click from " + webAdCampaignTable + " c, " + webAdCampaignHistoryTable + " ch " +
                        "where c.campaign_id=ch.campaign_id\n" +
                        (admobCheck ? " " : "and date between '" + startTime + "' and '" + endTime + "' " )+
                        "and c.campaign_id in (" + campaignIds + ")" +
                        "group by ch.campaign_id, country_code) a " + orderStr;
            }
            list = DB.findListBySql(sql);
        } else {
            list.clear();
        }
        JsonObject jsonObject = new JsonObject();
        JsonArray array = new JsonArray();
        double total_spend = 0;
        double total_installed = 0;
        double total_impressions = 0;
        double total_click = 0;
        double total_ctr = 0;
        double total_cpa = 0;
        double total_cvr = 0;
        for (int i = 0; i < list.size(); i++) {
            JSObject one = list.get(i);
            String campaign_id = one.get("campaign_id");
            String account_id = one.get("account_id");
            String campaign_name = one.get("campaign_name");
            String status = one.get("status");
            String create_time = one.get("create_time").toString();
            String country_code = one.get("country_code");
            double budget = one.get("budget");
            double bidding = one.get("bidding");
            double spend = NumberUtil.convertDouble(one.get("spend"), 0);
            double installed = NumberUtil.convertDouble(one.get("installed"), 0);
            double impressions = NumberUtil.convertDouble(one.get("impressions"), 0);
            double click = NumberUtil.convertDouble(one.get("click"), 0);
            double ctr = impressions > 0 ? click / impressions : 0;
            double cpa = installed > 0 ? spend / installed : 0;
            double cvr = click > 0 ? installed / click : 0;
            total_spend += spend;
            total_installed += installed;
            total_impressions += impressions;
            total_click += click;
            total_ctr = total_impressions > 0 ? total_click / total_impressions : 0;
            total_cpa = total_installed > 0 ? total_spend / total_installed : 0;
            total_cvr = total_click > 0 ? total_installed / total_click : 0;
            if ("true".equals(emptyCampaign) && spend > 0) {
                continue;
            }

            JsonObject d = new JsonObject();
            d.addProperty("campaign_id", campaign_id);
            d.addProperty("account_id", account_id);
            d.addProperty("campaign_name", campaign_name);
            d.addProperty("status", status);
            d.addProperty("create_time", create_time);
            d.addProperty("country_code", country_code);
            d.addProperty("country_name", countryMap.get(country_code));
            d.addProperty("budget", budget);
            d.addProperty("bidding", bidding);
            d.addProperty("impressions", impressions);
            d.addProperty("spend", spend);
            d.addProperty("installed", installed);
            d.addProperty("click", click);
            d.addProperty("ctr", NumberUtil.trimDouble(ctr,4));
            d.addProperty("cpa", NumberUtil.trimDouble(cpa,4));
            d.addProperty("cvr", NumberUtil.trimDouble(cvr,4));
            array.add(d);
        }
        jsonObject.add("array", array);
        jsonObject.addProperty("total_spend", total_spend);
        jsonObject.addProperty("total_installed", total_installed);
        jsonObject.addProperty("total_impressions", total_impressions);
        jsonObject.addProperty("total_click", total_click);
        jsonObject.addProperty("total_ctr", NumberUtil.trimDouble(total_ctr,4));
        jsonObject.addProperty("total_cpa", NumberUtil.trimDouble(total_cpa,4));
        jsonObject.addProperty("total_cvr", NumberUtil.trimDouble(total_cvr,4));
        return jsonObject;
    }
}
