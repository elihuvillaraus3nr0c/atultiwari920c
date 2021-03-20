/**
 * 
 */
package com.atul.pdf.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.itextpdf.html2pdf.HtmlConverter;

/**
 * @author Atul Tiwari
 *
 */

@Controller
public class HomeController {
	final private static String path = "E:/HtmlViewInPDF";
	
	@Autowired
	HttpServletResponse respon;
	
	@RequestMapping("/")
	public String home() {
		return "index";
	}
	
	@RequestMapping(value = "/generatePDF", method = RequestMethod.POST)
	public void generatePDF(@RequestParam("htmlString") String htmlString,@RequestParam("fileName") String fileName,HttpServletResponse response) {
		try {
			File folder = new File(path);
			if(!folder.exists())
				folder.mkdirs();
			String filePath = path + "/" +fileName+".pdf";
			HtmlConverter.convertToPdf(htmlString, new FileOutputStream(filePath));
			File file  = new File(filePath);
			if(file.exists()) {
				response.setContentType("application/octet-stream");
				response.addHeader("Content-Disposition", "attachement; fileName="+file.getName());
				Files.copy(file.toPath(),response.getOutputStream());
				response.getOutputStream().flush();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@RequestMapping(value = "/generatePDFFromFile", method = RequestMethod.POST)
	public void generatePDF(@RequestParam("fileName") String fileName,@RequestParam("htmlFile") MultipartFile htmlFile,HttpServletResponse response) {
		try {
			File folder = new File(path);
			if(!folder.exists())
				folder.mkdirs();
			String filePath = path + "/" +fileName+".pdf";
			HtmlConverter.convertToPdf(new String(htmlFile.getBytes()), new FileOutputStream(filePath));
			File file  = new File(filePath);
			if(file.exists()) {
				response.setContentType("application/octet-stream");
				response.addHeader("Content-Disposition", "attachement; fileName="+file.getName());
				Files.copy(file.toPath(),response.getOutputStream());
				response.getOutputStream().flush();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
