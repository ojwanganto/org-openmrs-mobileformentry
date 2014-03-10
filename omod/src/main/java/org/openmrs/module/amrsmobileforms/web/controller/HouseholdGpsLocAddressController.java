package org.openmrs.module.amrsmobileforms.web.controller;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.amrsmobileforms.MobileFormEntryService;
import org.openmrs.module.amrsmobileforms.MobileFormHousehold;
import org.openmrs.module.amrsmobileforms.Survey;
import org.openmrs.module.amrsmobileforms.util.MobileFormEntryUtil;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: alfayo
 * Date: 9/2/13
 * Time: 2:54 PM
 * To change this template use File | Settings | File Templates.
 */
@Controller
public class HouseholdGpsLocAddressController {


    @RequestMapping(method= RequestMethod.GET, value="module/amrsmobileforms/addressView.list")
    public void pageLoad(ModelMap map){

       }



    @RequestMapping("/module/amrsmobileforms/addresses.json")
    public @ResponseBody
    Map<String, Object> getErrorBatchAsJson(
            @RequestParam("iDisplayStart") int iDisplayStart,
            @RequestParam("iDisplayLength") int iDisplayLength,
            @RequestParam("sSearch") String sSearch,
            @RequestParam("sEcho") int sEcho) throws IOException {

        // get the data

        List<Survey> listMobileFormHouseholdSurveys=Context.getService(MobileFormEntryService.class).getBatchMobileFormHouseHoldSurveys(iDisplayStart,iDisplayLength,sSearch) ;
        List<Object> listFormHouseholds = new ArrayList<Object>();
        for (Survey householdSurvey : listMobileFormHouseholdSurveys) {
            listFormHouseholds.add(generateObjectMap(householdSurvey));
        }
        // build the response

        Map<String, Object> response;
        response = new HashMap<String, Object>();
        response.put("iTotalRecords", Context.getService(MobileFormEntryService.class).countHouseholdSurveys(null));
        response.put("iTotalDisplayRecords",Context.getService(MobileFormEntryService.class).countHouseholdSurveys(sSearch));
        response.put("sEcho", sEcho);
        response.put("aaData", listFormHouseholds.toArray());

        // send it
        return response;


    }


    private Map<String, Object> generateObjectMap(Survey survey) {
        String gps=survey.getHousehold().getGpsLocation();
        String[] sa = survey.getHousehold().getGpsLocation().split(" ");
        String longitude=MobileFormEntryUtil.formatGps(Double.parseDouble(sa[1]),"lon");
        String latitude= MobileFormEntryUtil.formatGps(Double.parseDouble(sa[0]), "lat");

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("id", survey.getId());
        result.put("providerId", survey.getProviderId());
        result.put("village", survey.getHousehold().getVillage());
        result.put("household_identifier",survey.getHousehold().getHouseholdIdentifier());
        result.put("district", survey.getHousehold().getDistrict());
        result.put("location", survey.getHousehold().getLocation());
        result.put("sublocation", survey.getHousehold().getSublocation());
        result.put("longitude",longitude);
        result.put("latitude", latitude);
        return result;
    }



        @RequestMapping(value="module/amrsmobileforms/addressdownload")
        public void downloadCSV( HttpServletResponse response,HttpServletRequest request,
                @RequestParam(required=false, value="search") String searchStr
        ) throws IOException {


            //lengthen the session to make sure generation is complete
            HttpSession httpSession=request.getSession();
            Integer httpSessionvalue=httpSession.getMaxInactiveInterval();
            httpSession.setMaxInactiveInterval(-1);
            //create a temporary file that will be destroyed after completion
            File file= File.createTempFile("PHCT_Household",".csv");


            StringBuilder stringBuilderColumnsHeaders=new StringBuilder();
            StringBuilder stringBuilderContents;

            StringBuilder reportHeader=new StringBuilder();




            reportHeader.append("#"+"PHCT"+"#"+"," +"#"+"HOUSEHOLD"+ "#"+","+"#"+"LOCATIONS"+"#"+",");

            stringBuilderColumnsHeaders.append("#Provider Id").append(",").append("#Village#").
                    append("#Household Identifier#").append(",").append("#District#").append(",").append("#Location#").append(",").append("#Sub Location#").append(",").append("#Longitude#").append(",").append("#Latitude#").append(",");

            FileWriter fstream = new FileWriter(file,true);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(reportHeader.toString()+"\n");
            out.write(stringBuilderColumnsHeaders.toString()+"\n");
            int startAt=0;

           Number maxrows=(Context.getService(MobileFormEntryService.class).countHouseholdSurveys(searchStr));
            List<Survey> listMobileFormHouseholds=Context.getService(MobileFormEntryService.class).getBatchMobileFormHouseHoldSurveys(startAt, maxrows.intValue(), searchStr) ;
            for (Survey householdSurvey : listMobileFormHouseholds) {

                stringBuilderContents=new StringBuilder();
                String longitude=null;
                String latitude= null;
                String[] sa = householdSurvey.getHousehold().getGpsLocation().split(" ");

                if((householdSurvey.getHousehold().getGpsLocation().contains("\""))||(householdSurvey.getHousehold().getGpsLocation().contains("\'"))){
                        longitude=sa[1];
                        latitude= sa[0];

                }
                else{
                    //format if the gps was not formatted
                     if(sa.length==2){
                        if((!StringUtils.isEmpty(sa[0]))&&(!StringUtils.isEmpty(sa[1]))){
                        longitude= MobileFormEntryUtil.formatGps(Double.parseDouble(sa[1]),"lon");
                        latitude=  MobileFormEntryUtil.formatGps(Double.parseDouble(sa[0]), "lat");
                        }
                     } else {

                         longitude="Invalid GPS "+householdSurvey.getHousehold().getGpsLocation();
                         latitude= "Invalid GPS "+householdSurvey.getHousehold().getGpsLocation();
                     }
                }
                stringBuilderContents.append("#"+householdSurvey.getProviderId()+"#").append(",").append("#"+householdSurvey.getHousehold().getVillage()).append("#"+householdSurvey.getHousehold().getHouseholdIdentifier()+"#").append(",").append("#"+householdSurvey.getHousehold().getDistrict()+"#").append(",").append("#"+householdSurvey.getHousehold().getLocation()+"#").append(",").append("#"+householdSurvey.getHousehold().getSublocation()+"#").append(",").append("#"+longitude+"#").append(",").append("#"+latitude+"#").append(",");
                out.write(stringBuilderContents.toString());
                out.write("\n");

            }

            out.close();
            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-Disposition", "attachment; filename=" + file);
            response.setContentLength((int) file.length());
            FileCopyUtils.copy(new FileInputStream(file), response.getOutputStream());
            file.delete();
            httpSession.setMaxInactiveInterval(httpSessionvalue);
        }

}

