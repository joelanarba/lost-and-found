package com.lfms.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

public class PdfReportService {

    public static Path generateMonthlyReport(int totalUsers, int openLost, int openFound, int pendingClaims, int resolvedItems) {
        Path outPath = Paths.get(System.getProperty("user.home"), "Desktop", "LFMS_Report_" + LocalDate.now() + ".pdf");
        
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("UCC LFMS - Monthly Admin Report");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 14);
                contentStream.newLineAtOffset(50, 650);
                contentStream.setLeading(20.0f);
                
                contentStream.showText("Date Generated: " + LocalDate.now());
                contentStream.newLine();
                contentStream.showText("--------------------------------------------------");
                contentStream.newLine();
                contentStream.showText("Total Users: " + totalUsers);
                contentStream.newLine();
                contentStream.showText("Open Lost Items: " + openLost);
                contentStream.newLine();
                contentStream.showText("Open Found Items: " + openFound);
                contentStream.newLine();
                contentStream.showText("Pending Claims: " + pendingClaims);
                contentStream.newLine();
                contentStream.showText("Resolved Items: " + resolvedItems);
                contentStream.newLine();
                
                contentStream.endText();
            }
            
            document.save(outPath.toFile());
            return outPath;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Path generateCertificate(com.lfms.model.Claim claim, com.lfms.model.Item item) {
        Path outPath = Paths.get(System.getProperty("user.home"), "Desktop", "LFMS_Certificate_" + claim.getClaimId() + ".pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 24);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("Item Recovery Certificate");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 14);
                contentStream.newLineAtOffset(50, 650);
                contentStream.setLeading(20.0f);
                
                contentStream.showText("Claim ID: CLM-" + claim.getClaimId());
                contentStream.newLine();
                contentStream.showText("Date Approved: " + LocalDate.now());
                contentStream.newLine();
                contentStream.showText("--------------------------------------------------");
                contentStream.newLine();
                contentStream.showText("This certifies that the item:");
                contentStream.newLine();
                contentStream.showText("Name: " + item.getName());
                contentStream.newLine();
                contentStream.showText("Category: " + item.getCategory());
                contentStream.newLine();
                contentStream.newLine();
                contentStream.showText("Has been successfully recovered and ownership verified to:");
                contentStream.newLine();
                contentStream.showText("Claimant: " + claim.getClaimantName() + " (" + (claim.getClaimantIndexNo() != null ? claim.getClaimantIndexNo() : "") + ")");
                contentStream.newLine();
                contentStream.showText("Email: " + claim.getClaimantEmail());
                contentStream.newLine();
                
                contentStream.endText();
            }
            
            document.save(outPath.toFile());
            return outPath;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
