package com.bestgo.admanager.servlet;

import com.bestgo.admanager.constant.JedisConstant;
import com.bestgo.admanager.utils.DateUtil;
import com.bestgo.admanager.utils.JedisPoolUtil;
import com.bestgo.admanager.utils.NumberUtil;
import com.bestgo.admanager.utils.Utils;
import com.bestgo.common.database.services.DB;
import com.bestgo.common.database.utils.JSObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import redis.clients.jedis.Jedis;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * @author mengjun
 * @date 2018/2/2 21:15
 * @desc 分析每个应用每个国家的活跃用户度的报告
 */
@WebServlet(name = "ActiveUserReport", urlPatterns = {"/active_user_report/*"})
public class ActiveUserReport extends BaseHttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.doPost(request, response);
        if (!Utils.isAdmin(request, response)) return;
        Jedis jedis = JedisPoolUtil.getJedis();
        String path = request.getPathInfo();
        JsonObject jsonObject = new JsonObject();
        String tagName = request.getParameter("tagName");
        String sorterId = request.getParameter("sorterId");


        if (path.matches("/query_active_user_report")) {
            List<JSObject> list = null;
            JsonArray jsonArray = new JsonArray();
            try {
                String today = DateUtil.getNowDate();
                String yesterday = DateUtil.addDay(today,-1,"yyyy-MM-dd");//美国今天
                String sevenDayAgo = DateUtil.addDay(today,-7,"yyyy-MM-dd");//美国七天前
                String sql = "select total_installeds,country_code,avg_7_day_active,avg_14_day_active,avg_30_day_active,avg_60_day_active, " +
                        "seven_days_data_update_date,fourteen_days_data_update_date,thirty_days_data_update_date,sixty_days_data_update_date " +
                        " from ad_report_active_user_admob_rel_result where tag_name = '" + tagName + "' ";
                int sorter = 0;
                if (sorterId != null) {
                    sorter = NumberUtil.parseInt(sorterId, 0);
                    switch(sorter) {
                        case 2090:
                            sql += " order by total_installeds desc";
                            break;
                        case 90:
                            sql += " order by total_installeds";
                            break;
                        case 2091:
                            sql += " order by avg_7_day_active desc";
                            break;
                        case 91:
                            sql += " order by avg_7_day_active";
                            break;
                        case 2092:
                            sql += " order by avg_14_day_active desc";
                            break;
                        case 92:
                            sql += " order by avg_14_day_active";
                            break;
                        case 2093:
                            sql += " order by avg_30_day_active desc";
                            break;
                        case 93:
                            sql += " order by avg_30_day_active";
                            break;
                        case 2094:
                            sql += " order by avg_60_day_active desc";
                            break;
                        case 94:
                            sql += " order by avg_60_day_active";
                            break;
                    }
                }
                list = DB.findListBySql(sql);
                JSObject one = null;
                for (JSObject j : list) {
                    if(j.hasObjectData()){
                        JsonObject jo = new JsonObject();
                        String countryCode = j.get("country_code");
                        String countryName = jedis.hget(JedisConstant.COUNTRY_CODE_NAME_MAP,countryCode);
                        if (countryName == null) {
                            one = DB.findOneBySql("SELECT country_name FROM app_country_code_dict WHERE country_code = '" + countryCode + "'");
                            if (one.hasObjectData()) {
                                countryName = one.get("country_name");
                                jedis.hset(JedisConstant.COUNTRY_CODE_NAME_MAP,countryCode,countryName);
                            } else {
                                countryName = "--";
                            }
                        }
                        double sevenDaysAvgARPU = 0;
                        sql = "SELECT avg(arpu) AS seven_days_avg_arpu FROM web_ad_country_analysis_report_history h,web_facebook_app_ids_rel r " +
                                "WHERE h.app_id = r.google_package_id AND tag_name = '" + tagName + "' AND h.country_code = '" +
                                countryCode + "' AND date BETWEEN '" + sevenDayAgo + "' and '" + yesterday + "'";
                        one = DB.findOneBySql(sql);
                        if(one.hasObjectData()){
                            sevenDaysAvgARPU = NumberUtil.trimDouble(NumberUtil.convertDouble(one.get("seven_days_avg_arpu"),0),3);
                        }
                        double totalInstalleds = NumberUtil.trimDouble(NumberUtil.convertDouble(j.get("total_installeds"), 0),3);
                        double avgSevenDayActive = NumberUtil.trimDouble(NumberUtil.convertDouble(j.get("avg_7_day_active"), 0),3);
                        double avgFourteenDayActive = NumberUtil.trimDouble(NumberUtil.convertDouble(j.get("avg_14_day_active"), 0),3);
                        double avgThirtyDayActive = NumberUtil.trimDouble(NumberUtil.convertDouble(j.get("avg_30_day_active"), 0),3);
                        double avgSixtyDayActive = NumberUtil.trimDouble(NumberUtil.convertDouble(j.get("avg_60_day_active"), 0),3);

                        //业务逻辑上，这里不应该出现空值，所以不用判断
                        Date sevenDaysDataUpdateDate = j.get("seven_days_data_update_date");
                        Date fourteenDaysDataUpdateDate = j.get("fourteen_days_data_update_date");
                        Date thirtyDaysDataUpdateDate = j.get("thirty_days_data_update_date");
                        Date sixtyDaysDataUpdateDate = j.get("sixty_days_data_update_date");

                        jo.addProperty("country_name", countryName);
                        jo.addProperty("total_installeds", totalInstalleds);
                        jo.addProperty("avg_7_day_active", avgSevenDayActive);
                        jo.addProperty("avg_14_day_active", avgFourteenDayActive);
                        jo.addProperty("avg_30_day_active", avgThirtyDayActive);
                        jo.addProperty("avg_60_day_active", avgSixtyDayActive);
                        jo.addProperty("seven_days_avg_arpu", sevenDaysAvgARPU);

                        jo.addProperty("seven_days_data_update_date", sevenDaysDataUpdateDate.toString());
                        jo.addProperty("fourteen_days_data_update_date", fourteenDaysDataUpdateDate.toString());
                        jo.addProperty("thirty_days_data_update_date", thirtyDaysDataUpdateDate.toString());
                        jo.addProperty("sixty_days_data_update_date", sixtyDaysDataUpdateDate.toString());

                        jo.addProperty("avg_7_day_active_mul_arpu", NumberUtil.trimDouble(avgSevenDayActive * sevenDaysAvgARPU,3));
                        jo.addProperty("avg_14_day_active_mul_arpu", NumberUtil.trimDouble(avgFourteenDayActive * sevenDaysAvgARPU,3));
                        jo.addProperty("avg_30_day_active_mul_arpu", NumberUtil.trimDouble(avgThirtyDayActive * sevenDaysAvgARPU,3));
                        jo.addProperty("avg_60_day_active_mul_arpu", NumberUtil.trimDouble(avgSixtyDayActive * sevenDaysAvgARPU,3));
                        jsonArray.add(jo);
                    }
                }

                jsonObject.add("array", jsonArray);
                jsonObject.addProperty("ret", 1);
                jsonObject.addProperty("message", "执行成功");
            } catch (Exception e) {
                jsonObject.addProperty("ret", 0);
                jsonObject.addProperty("message", e.getMessage());
            }
        }
        if (jedis != null) {
            jedis.close();
        }
        response.getWriter().write(jsonObject.toString());
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }
}
