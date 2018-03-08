package com.bestgo.admanager.servlet;

import com.bestgo.admanager.DateUtil;
import com.bestgo.admanager.Utils;
import com.bestgo.common.database.services.DB;
import com.bestgo.common.database.utils.JSObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * 国家分析报告
 */
@WebServlet(name = "CountryAnalysisReport", urlPatterns = {"/country_analysis_report/*"}, asyncSupported = true)
public class CountryAnalysisReport extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!Utils.isAdmin(request, response)) return;

        String path = request.getPathInfo();
        JsonObject jsonObject = new JsonObject();
        String sorterId = request.getParameter("sorterId");
        String tagName = request.getParameter("tagName");
        String startTime = request.getParameter("startTime");
        String endTime = request.getParameter("endTime");
        String sevenDaysAgo = DateUtil.addDay(endTime,-6,"yyyy-MM-dd");//包括endTime
        String fourteenDaysAgo = DateUtil.addDay(endTime,-13,"yyyy-MM-dd");//包括endTime

        String beforeTenDay = DateUtil.addDay(endTime,-10,"yyyy-MM-dd");//不包括endTime
        String beforeFourDay = DateUtil.addDay(endTime,-4,"yyyy-MM-dd");//不包括endTime
        String beforeTwentyTwoDay = DateUtil.addDay(endTime,-22,"yyyy-MM-dd");//不包括endTime
        if (path.matches("/query_country_analysis_report")) {
            try {
                String sqlG = "select google_package_id from web_facebook_app_ids_rel WHERE tag_name = '" + tagName + "'";
                JSObject oneG = DB.findOneBySql(sqlG);
                if(oneG != null){
                    String appId = oneG.get("google_package_id");
                    if(appId != null){
                        JsonArray jsonArray = new JsonArray();
                        String sql = "select country_code, sum(cost) as total_cost, sum(purchased_user) as total_purchased_user, " +
                                "sum(total_installed) as installed, sum(today_uninstalled) as total_today_uninstalled, " +
                                "sum(total_user) as users, sum(active_user) as active_users, sum(impression) as impressions, sum(revenue) as revenues, " +
                                " (sum(revenue) - sum(cost)) as incoming, "+
                                "(case when sum(impression) > 0 then sum(revenue) * 1000 / sum(impression) else 0 end) as ecpm,"+
                                "(case when sum(purchased_user) > 0 then sum(cost) / sum(purchased_user) else 0 end) as cpa "+
                                "from web_ad_country_analysis_report_history where app_id = '" + appId +"' " +
                                "and date BETWEEN '" + startTime + "' AND '" + endTime + "' GROUP BY country_code";

                        int sorter = 0;
                        if (sorterId != null) {
                            sorter = Utils.parseInt(sorterId, 0);
                        }
                        switch(sorter){
                            case 1031:
                                sql += " order by total_cost desc";
                                break;
                            case 31:
                                sql += " order by total_cost";
                                break;
                            case 1033:
                                sql += " order by total_purchased_user desc";
                                break;
                            case 33:
                                sql += " order by total_purchased_user";
                                break;
                            case 1034:
                                sql += " order by installed desc";
                                break;
                            case 34:
                                sql += " order by installed";
                                break;
                            case 1037:
                                sql += " order by users desc";
                                break;
                            case 37:
                                sql += " order by users";
                                break;
                            case 1038:
                                sql += " order by active_users desc";
                                break;
                            case 38:
                                sql += " order by active_users";
                                break;
                            case 1039:
                                sql += " order by revenues desc";
                                break;
                            case 39:
                                sql += " order by revenues";
                                break;
                            case 1040:
                                sql += " order by ecpm desc";
                                break;
                            case 40:
                                sql += " order by ecpm";
                                break;
                            case 1041:
                                sql += " order by cpa desc";
                                break;
                            case 41:
                                sql += " order by cpa";
                                break;
                            case 1042:
                                sql += " order by incoming desc";
                                break;
                            case 42:
                                sql += " order by incoming";
                                break;
                            default:
                                sql += " order by total_cost desc";

                        }
                        List<JSObject> countryDetailJSObjectList = DB.findListBySql(sql);

                        double totalCost = 0;
                        double totalPuserchaedUser = 0;
                        double totalRevenue = 0;


                        for(JSObject j : countryDetailJSObjectList){
                            if(j.hasObjectData()){
                                String countryCode = j.get("country_code");


                                //计算七天的总花费、总营收、总盈利等
                                sql = "select cost, revenue,purchased_user,impression " +
                                        "from web_ad_country_analysis_report_history where app_id = '" + appId + "' " +
                                        " and country_code = '" + countryCode + "' and date BETWEEN '" + sevenDaysAgo + "' AND '" + endTime + "'";
                                List<JSObject> listCR = DB.findListBySql(sql);

                                double sevenDaysCosts  = 0;
                                double sevenDaysRevenues = 0;
                                double sevenDaysImpressions = 0;
                                for(JSObject one : listCR){
                                    if(one.hasObjectData()){
                                        double cost = Utils.convertDouble(one.get("cost"), 0);
                                        double revenue = Utils.convertDouble(one.get("revenue"), 0);
                                        double impression = Utils.convertDouble(one.get("impression"), 0);
                                        sevenDaysCosts  += cost;
                                        sevenDaysRevenues  += revenue;
                                        sevenDaysImpressions  += impression;
                                    }
                                }
                                double sevenDaysAvgEcpm = sevenDaysImpressions == 0 ? 0 : sevenDaysRevenues * 1000 / sevenDaysImpressions;
                                double sevenDaysIncoming = sevenDaysRevenues - sevenDaysCosts;


                                //悬浮显示十四天的总花费、总营收、总盈利等
                                sql = "select date,cost, purchased_user,total_installed,revenue,active_user, " +
                                        "(case when total_installed > 0 then today_uninstalled / total_installed else 0 end) as uninstall_rate," +
                                        "(case when impression > 0 then revenue * 1000 / impression else 0 end) as ecpm," +
                                        "(case when purchased_user > 0 then cost / purchased_user else 0 end) as cpa " +
                                        "from web_ad_country_analysis_report_history where app_id = '" + appId + "' " +
                                        " and country_code = '" + countryCode + "' and date BETWEEN '" + fourteenDaysAgo + "' AND '" + endTime + "'";
                                listCR = DB.findListBySql(sql);
                                String everyDayCostForFourteenDays = "";
                                String everyDayPurchasedUserForFourteenDays = "";
                                String everyDayInstalledForFourteenDays = "";
                                String everyDayUninstalledRateForFourteenDays = "";
                                String everyDayActiveUserForFourteenDays = "";
                                String everyDayRevenueForFourteenDays = "";
                                String everyDayEcpmForFourteenDays = "";
                                String everyDayCpaForFourteenDays = "";
                                String everyDayCpaDevEcpmForFourteenDays = "";
                                String everyDayIncomingForFourteenDays = "";
                                for(JSObject one : listCR){
                                    if(one.hasObjectData()){
                                        Date date = one.get("date");
                                        double cost = Utils.convertDouble(one.get("cost"), 0);
                                        double purchasedUser = Utils.convertDouble(one.get("purchased_user"), 0);
                                        double installed = Utils.convertDouble(one.get("total_installed"), 0);
                                        double uninstallRate = Utils.convertDouble(one.get("uninstall_rate"), 0);
                                        double activeUser = Utils.convertDouble(one.get("active_user"), 0);
                                        double revenue = Utils.convertDouble(one.get("revenue"), 0);
                                        double ecpm = Utils.convertDouble(one.get("ecpm"), 0);
                                        double cpa = Utils.convertDouble(one.get("cpa"), 0);
                                        double cpaDevEcpm = ecpm == 0 ? 0 : cpa / ecpm;
                                        double incoming = revenue - cost;
                                        everyDayCostForFourteenDays += date + "("+ (int)cost + ")" + "\n";
                                        everyDayPurchasedUserForFourteenDays +=  date + "("+ (int)purchasedUser + ")" + "\n";
                                        everyDayInstalledForFourteenDays +=  date + "("+ (int)installed + ")" + "\n";
                                        everyDayUninstalledRateForFourteenDays +=  date + "("+ Utils.trimDouble(uninstallRate,3) + ")" + "\n";
                                        everyDayActiveUserForFourteenDays +=  date + "("+ (int)activeUser + ")" + "\n";
                                        everyDayRevenueForFourteenDays +=  date + "("+ (int)revenue + ")" + "\n";
                                        everyDayEcpmForFourteenDays +=  date + "("+ Utils.trimDouble(ecpm,3) + ")" + "\n";
                                        everyDayCpaForFourteenDays +=  date + "("+ Utils.trimDouble(cpa,3) + ")" + "\n";
                                        everyDayCpaDevEcpmForFourteenDays +=  date + "("+ Utils.trimDouble(cpaDevEcpm,3) + ")" + "\n";
                                        everyDayIncomingForFourteenDays +=  date + "("+ (int)incoming + ")" + "\n";
                                    }
                                }

                                sql = "select date, pi from web_ad_country_analysis_report_history_by_date where app_id = '" + appId + "' " +
                                        " and country_code = '" + countryCode + "' and date BETWEEN '" + beforeTwentyTwoDay + "' AND '" + beforeFourDay + "'";
                                listCR = DB.findListBySql(sql);
                                String everyDayPiForFourteenDays = "";
                                if(listCR != null && listCR.size()>0){
                                    for(JSObject one : listCR){
                                        double pi = Utils.convertDouble(one.get("pi"), 0);
                                        Date date = one.get("date");
                                        everyDayPiForFourteenDays += date + "(" + Utils.trimDouble(pi,3) + ")" + "\n";
                                    }
                                }

                                sql = "select pi,a_cpa " +
                                        " from web_ad_country_analysis_report_history where app_id = '" + appId + "' " +
                                        " and country_code = '" + countryCode + "' and date = '" + endTime + "'";
                                JSObject oneC = DB.findOneBySql(sql);
                                double pi = 0;
                                double aCpa = 0;
                                if(oneC.hasObjectData()){
                                    pi = Utils.convertDouble(oneC.get("pi"),0);
                                    aCpa = Utils.convertDouble(oneC.get("a_cpa"),0);
                                }

                                double revenues = Utils.convertDouble(j.get("revenues"),0);
                                double ecpm = Utils.convertDouble(j.get("ecpm"),0);
                                sql = "select country_name from app_country_code_dict where country_code = '" + countryCode + "'";
                                oneC = DB.findOneBySql(sql);
                                String countryName = "";
                                if(oneC.hasObjectData()){
                                    countryName = oneC.get("country_name");
                                }else{
                                    countryName = countryCode;
                                }
                                double costs = Utils.convertDouble(j.get("total_cost"),0);
                                double purchasedUsers = Utils.convertDouble(j.get("total_purchased_user"),0);
                                double installed = Utils.convertDouble(j.get("installed"),0);
                                double totalTodayUninstalled = Utils.convertDouble(j.get("total_today_uninstalled"),0);
                                double uninstalledRate = installed != 0 ? totalTodayUninstalled / installed : 0;


                                double users = Utils.convertDouble(j.get("users"),0);
                                double activeUsers = Utils.convertDouble(j.get("active_users"),0);


                                double cpa = Utils.convertDouble(j.get("cpa"),0);
                                double incoming = Utils.convertDouble(j.get("incoming"),0);
                                double cpaDevEcpm = (ecpm == 0) ? 0 : (cpa / ecpm);

                                sql = "SELECT avg(pi) as avg_pi FROM web_ad_country_analysis_report_history_by_date " +
                                        "WHERE app_id = '" + appId + "' AND country_code = '" + countryCode + "' AND date BETWEEN '" + beforeTenDay + "' AND '" + beforeFourDay + "'";
                                oneC = DB.findOneBySql(sql);
                                double sevenDaysAvgPi = 0;
                                if(oneC.hasObjectData()){
                                    sevenDaysAvgPi = Utils.convertDouble(oneC.get("avg_pi"),0);
                                }

                                //RT回报时长=CPA * 1000 / sevenDaysAvgPi / sevenDaysAvgEcpm
                                double rt = (sevenDaysAvgPi == 0 || sevenDaysAvgEcpm == 0) ? 0 : (cpa * 1000 / sevenDaysAvgPi / sevenDaysAvgEcpm);

                                totalCost += costs;
                                totalPuserchaedUser += purchasedUsers;
                                totalRevenue += revenues;

                                JsonObject d = new JsonObject();
                                d.addProperty("country_name", countryName);
                                d.addProperty("costs", Utils.trimDouble(costs,0));
                                d.addProperty("purchased_users", purchasedUsers);
                                d.addProperty("installed", installed);
                                d.addProperty("uninstalled_rate", Utils.trimDouble(uninstalledRate,3));
                                d.addProperty("users", users);
                                d.addProperty("active_users", activeUsers);
                                d.addProperty("revenues", Utils.trimDouble(revenues,0));
                                d.addProperty("pi", Utils.trimDouble(pi,3));
                                d.addProperty("ecpm", Utils.trimDouble(ecpm,3));
                                d.addProperty("cpa_dev_ecpm", Utils.trimDouble(cpaDevEcpm,3));
                                d.addProperty("seven_days_costs", Utils.trimDouble(sevenDaysCosts,0));
                                d.addProperty("seven_days_incoming", Utils.trimDouble(sevenDaysIncoming,0));
                                d.addProperty("seven_days_revenues", Utils.trimDouble(sevenDaysRevenues,0));


                                d.addProperty("every_day_cost_for_fourteen_days", everyDayCostForFourteenDays);
                                d.addProperty("every_day_purchased_user_for_fourteen_days", everyDayPurchasedUserForFourteenDays);
                                d.addProperty("every_day_installed_for_fourteen_days", everyDayInstalledForFourteenDays);
                                d.addProperty("every_day_uninstalled_rate_for_fourteen_days", everyDayUninstalledRateForFourteenDays);
                                d.addProperty("every_day_active_user_for_fourteen_days", everyDayActiveUserForFourteenDays);
                                d.addProperty("every_day_revenue_for_fourteen_days", everyDayRevenueForFourteenDays);
                                d.addProperty("every_day_pi_for_fourteen_days", everyDayPiForFourteenDays);
                                d.addProperty("every_day_ecpm_for_fourteen_days", everyDayEcpmForFourteenDays);
                                d.addProperty("every_day_cpa_for_fourteen_days", everyDayCpaForFourteenDays);
                                d.addProperty("every_day_cpa_dev_ecpm_for_fourteen_days", everyDayCpaDevEcpmForFourteenDays);
                                d.addProperty("every_day_incoming_for_fourteen_days", everyDayIncomingForFourteenDays);

                                d.addProperty("a_cpa", Utils.trimDouble(aCpa,3));
                                d.addProperty("incoming", Utils.trimDouble(incoming,0));
                                d.addProperty("cpa", Utils.trimDouble(cpa,3));
                                d.addProperty("rt", Utils.trimDouble(rt,3));
                                jsonArray.add(d);
                            }

                        }
                        double total_cpa = totalPuserchaedUser != 0 ? totalCost / totalPuserchaedUser : 0;
                        jsonObject.add("array", jsonArray);

                        jsonObject.addProperty("total_cost", Utils.trimDouble(totalCost,0));
                        jsonObject.addProperty("total_puserchaed_user", Utils.trimDouble(totalPuserchaedUser,0));
                        jsonObject.addProperty("total_cpa", Utils.trimDouble(total_cpa,3));
                        jsonObject.addProperty("total_revenue", Utils.trimDouble(totalRevenue,0));
                        jsonObject.addProperty("ret", 1);

                    }
                }

                jsonObject.addProperty("message", "执行成功");

            } catch (Exception e) {
                jsonObject.addProperty("ret", 0);
                jsonObject.addProperty("message", e.getMessage());
            }
        }else if(path.matches("/query_id_of_auto_create_campaigns")){
            try{
                String currCountryName = request.getParameter("curr_country_name");
                String sql = "select id from ad_campaigns_auto_create where app_name = '" + tagName + "' and country_region like '%" + currCountryName + "%'";
                JSObject oneF = DB.findOneBySql(sql);
                long facebookId = -1;
                if(oneF.hasObjectData()){
                    facebookId = oneF.get("id");
                }
                sql = "select country_code from app_country_code_dict where country_name = '" + currCountryName + "'";
                oneF = DB.findOneBySql(sql);
                String curr_country_code = null;
                if(oneF.hasObjectData()){
                    curr_country_code = oneF.get("country_code");
                }
                String sqlA = "select id from ad_campaigns_admob_auto_create where app_name = '" + tagName + "' and country_region like '%" + curr_country_code + "%'";
                JSObject oneA = DB.findOneBySql(sqlA);
                long id_adwords = -1;
                if(oneA != null && oneA.hasObjectData()){
                    id_adwords = oneA.get("id");
                }
                jsonObject.addProperty("id_facebook", facebookId);
                jsonObject.addProperty("id_adwords", id_adwords);
                jsonObject.addProperty("ret", 1);
                jsonObject.addProperty("message", "执行成功");
            } catch (Exception e) {
                jsonObject.addProperty("ret", 0);
                jsonObject.addProperty("message", e.getMessage());
            }
        }
        response.getWriter().write(jsonObject.toString());
    }
}