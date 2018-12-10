package cn.itcast.parse;


import org.apache.http.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by fudingcheng on 2018-11-17.
 */
public class Reptile {

    private static String loginURL="https://docs.emeter.com/dologin.action";
    private static String indexURL="https://docs.emeter.com/display/public/WELCOME/eMeter+Documentation";
    private static String baseURL="https://docs.emeter.com";
    private static String baseDIR_NAME="C:\\Users\\fudingcheng\\Desktop\\pdf";

    public static void main(String[] args) throws UnsupportedEncodingException {
        //设置Cookie对象
        CookieStore cookieStore = new BasicCookieStore();
        //获得httpClient
        CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build();
        System.out.println("--------------------登录开始-----------------------------------");
        //发起登录请求
        HttpPost loginPost = new HttpPost(loginURL);
        //封装登录参数
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("os_username", "qunlan.xu"));
        params.add(new BasicNameValuePair("os_password", "C@remark"));
        HttpEntity loginEntity = new UrlEncodedFormEntity(params, "UTF-8");
        loginPost.setEntity(loginEntity);
        //发起请求
        CloseableHttpResponse loginResponse = null;
        try {
            loginResponse = httpClient.execute(loginPost);
        } catch (IOException e) {
            System.err.println("Y(>_<、)Y,登录异常...");
        }
        //获得登录结果
        int loginCode = loginResponse.getStatusLine().getStatusCode();
        if(200==loginCode){
            System.out.println("登录成功");
        }
        //获得Cookie
        List<Cookie> cookies = cookieStore.getCookies();
        for (Cookie cookie:cookies) {
            System.out.println("Cookie=="+cookie.getName()+":"+cookie.getValue());
        }
        System.out.println("--------------------登录结束,爬虫开始--------------------------");
        //获得首页document
        Document document = null;
        try {
            document = getDocument(httpClient,indexURL);
            System.out.println("进入首页...");
        } catch (IOException e) {
            System.err.println("Y(>_<、)Y,首页打开异常...");
        }
        //解析首页document
        Element id_splitter_sidebar = document.select("#splitter-sidebar").get(0);
        Elements class_expand_containers = id_splitter_sidebar.select(".expand-container");
        //获得侧边栏所有的一级标题
        for (Element expand_container:class_expand_containers) {
            //一级标题名称
            String title_1 = expand_container.select(".expand-control-text").get(0).text();
            //System.out.println(title_1+":");
            //创建一级文件夹
            String title_1_dir_name=baseDIR_NAME+"\\"+title_1;
            File title_1_dir =new File(title_1_dir_name);
            if(!title_1_dir.exists()){
                title_1_dir.mkdirs();
                System.out.println("->进入【"+title_1+"】");
            }
            //侧边栏每个标题下的二级标题
            Elements as = expand_container.select("p").get(0).select("a");
            for (Element a:as) {
                //子栏目标题
                String title_2=a.text();
                //System.out.println("    "+"text:"+title_2);
                //System.out.println("    "+"href:"+a.attr("href"));
                //创建二级文件夹
                String title_2_dir_name = title_1_dir_name+"\\"+title_2;
                File title_2_dir = new File(title_1_dir_name);
                if(!title_2_dir.exists()){
                    title_2_dir.mkdirs();
                    System.out.println("    -->进入【"+title_2+"】");
                }
                //子栏目链接
                String subTitle_href=a.attr("href");
                //获得子栏目的document
                Document subDoc = null;
                try {
                    subDoc = getDocument(httpClient, baseURL + subTitle_href);
                } catch (IOException e) {
                    System.err.println("Y(>_<、)Y,侧边栏目录【"+title_2+"】打开异常");
                    continue; //程序继续...
                }
                //遍历每个文档的main目录
                Elements id_main_content_lis = subDoc.select("#main-content").select("ul").select("li");
                String title_2_main_name=title_2_dir_name+"\\"+"main";
                //创建二级目录下的main文件夹
                File title_2_main_dir = new File(title_2_main_name);
                if(!title_2_main_dir.exists()){
                    title_2_main_dir.mkdirs();
                    System.out.println("        --->进入【"+title_2+"】的（main）部分");
                }
                //System.err.println("        main:");
                for (Element main_li:id_main_content_lis) {
                    String doc_main_li_url = main_li.select("a").attr("href");
                    String doc_main_li_text = main_li.select("a").text();
                    //System.out.println("        "+"text:"+doc_main_li_text);
                    //System.out.println("        "+"href:"+doc_main_li_url);

                    try {
                        System.out.println("            ---->【"+title_2+"】main的"+doc_main_li_text+"开始下载");
                        downloadPDF(httpClient, title_2_main_dir, doc_main_li_url, doc_main_li_text);
                        System.out.println("            ---->【"+title_2+"】main的"+doc_main_li_text+"下载完毕");
                    } catch (IOException e) {
                        System.out.println("Y(>_<、)Y,【"+title_2_dir_name+"】的【"+doc_main_li_text+"】下载失败");
                        continue;   //程序继续...
                    }
                }
                System.out.println("        【"+title_2+"】的（main）部分下载完毕");
                //遍历每个文档的children目录
                //System.err.println("        children:");
                String title_2_children_name=title_2_dir_name+"\\"+"children";
                //创建二级目录下的child文件夹
                File title_2_children_dir = new File(title_2_children_name);
                if(!title_2_children_dir.exists()){
                    title_2_children_dir.mkdirs();
                    System.out.println("        --->进入【"+title_2+"】的（children）部分");
                }
                Elements id_childred_content_spans = subDoc.select("#children-section").select(".child-display");
                for (Element id_childred_content_span:id_childred_content_spans) {
                    String doc_child_url = id_childred_content_span.select("a").attr("href");
                    String doc_child_text = id_childred_content_span.select("a").text();
                    //System.out.println("        "+"text:"+doc_child_text);
                    //System.out.println("        "+"href:"+doc_child_url);
                    try {
                        System.out.println("            ---->【"+title_2+"】children的"+doc_child_text+"开始下载");
                        downloadPDF(httpClient,title_2_children_dir,doc_child_url,doc_child_text);
                        System.out.println("            ---->【"+title_2+"】children的"+doc_child_text+"下载完毕");
                    } catch (IOException e) {
                        System.out.println("Y(>_<、)Y,【"+title_2_dir_name+"】的【"+doc_child_text+"】下载失败");
                        continue;//程序继续...
                    }
                }
                System.out.println("        【"+title_2+"】的（children）部分下载完毕");
                System.out.println("    【"+title_2+"】下载完成");
            }
            System.out.println("【"+title_1_dir_name+"】下载完成");
        }


    }

    /**
     * 在指定页面,寻找pdf按钮的URL并执行pdf文件
     */
    private static void downloadPDF(CloseableHttpClient httpClient, File saveDir, String pageURL, String fileName) throws IOException {
        Document doc_main_li_doc = getDocument(httpClient, baseURL + pageURL);
        //获得文档页面中的导出pdf按钮
        Element id_action_export_pdf_link = doc_main_li_doc.select("#action-export-pdf-link").get(0);
        //导出pdf的链接
        String id_action_export_pdf_link_url = id_action_export_pdf_link.attr("href");

        CloseableHttpResponse response = httpClient.execute(new HttpGet(baseURL + id_action_export_pdf_link_url));
        InputStream inputStream = response.getEntity().getContent();
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(saveDir,fileName+".pdf")));
        byte[] bys=new byte[1024];
        int len;
        while((len=inputStream.read(bys))!=-1){
            bos.write(bys,0,len);
        }
        bos.flush();
        bos.close();
    }

    /**
     * 根据URL地址解析文档
     */
    private static Document getDocument(CloseableHttpClient httpClient,String url) throws IOException {
        //请求文档URL
        HttpGet docGet = new HttpGet(url);
        CloseableHttpResponse docResponse = httpClient.execute(docGet);
        //获得文档Doc
        String doc = EntityUtils.toString(docResponse.getEntity(),"UTF-8");
        //爬虫开始
        return Jsoup.parse(doc);
    }

}
