package service;

import model.FileInfoModel;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.BodyElementType;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import util.DateTimeUtil;
import util.FileUtil;

import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class FilePreviewService {
    private final HashService hashService = new HashService();

    public FileInfoModel readFileInfo(Path path) throws Exception {
        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
        FileInfoModel info = new FileInfoModel();
        info.setFileName(path.getFileName().toString());
        info.setFilePath(path.toAbsolutePath().toString());
        info.setFileType(resolveFileType(path));
        info.setFileSize(FileUtil.humanReadableSize(Files.size(path)));
        info.setCreatedAt(DateTimeUtil.displayMillis(attributes.creationTime().toMillis()));
        info.setLastModified(DateTimeUtil.displayMillis(attributes.lastModifiedTime().toMillis()));
        info.setSha256Hash(hashService.hashFile(path, "SHA-256"));
        info.setPreviewContent(readPreview(path));
        return info;
    }

    private String resolveFileType(Path path) throws IOException {
        String type = Files.probeContentType(path);
        if (type != null && !type.isBlank()) {
            return type;
        }
        String extension = FileUtil.extension(path);
        return extension.isBlank() ? "Không xác định" : "." + extension;
    }

    private String readPreview(Path path) {
        String extension = FileUtil.extension(path);
        try {
            return switch (extension) {
                case "txt", "json", "csv", "log" -> Files.readString(path, StandardCharsets.UTF_8);
                case "pdf" -> readPdf(path);
                case "docx" -> readDocx(path);
                case "xlsx" -> readXlsx(path);
                case "png", "jpg", "jpeg", "gif", "bmp" -> "Tệp ảnh: xem Thông tin tệp và Mã băm. Nội dung ký/xác thực dựa trên byte gốc của tệp.";
                default -> "Tệp không hỗ trợ xem trước nội dung. Hệ thống vẫn ký/xác thực dựa trên mã băm byte gốc.";
            };
        } catch (Exception ex) {
            return "Không thể đọc nội dung xem trước: " + ex.getMessage();
        }
    }

    private String readPdf(Path path) throws IOException {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            return new PDFTextStripper().getText(document);
        }
    }

    public List<Image> renderPdfPages(Path path, float dpi) throws IOException {
        List<Image> pages = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage bufferedImage = renderer.renderImageWithDPI(i, dpi, ImageType.RGB);
                pages.add(toFxImage(bufferedImage));
            }
        }
        return pages;
    }

    private String readDocx(Path path) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (InputStream inputStream = Files.newInputStream(path);
             XWPFDocument document = new XWPFDocument(inputStream)) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                builder.append(paragraph.getText()).append(System.lineSeparator());
            }
        }
        return builder.toString();
    }

    public DocxHtmlPreview readDocxHtml(Path path) {
        try (InputStream inputStream = Files.newInputStream(path);
             XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder html = new StringBuilder();
            html.append("<!doctype html><html><head><meta charset=\"UTF-8\"><style>")
                    .append("body{font-family:Segoe UI,Arial,sans-serif;margin:28px;color:#0f172a;line-height:1.45;}")
                    .append("h1,h2,h3{margin:16px 0 8px;} p{margin:8px 0;} ")
                    .append("table{border-collapse:collapse;margin:12px 0;width:100%;}td,th{border:1px solid #cbd5e1;padding:6px 8px;vertical-align:top;}")
                    .append("ul,ol{margin-top:6px;margin-bottom:6px;} .fallback{background:#fef3c7;border:1px solid #f59e0b;padding:10px;border-radius:6px;}")
                    .append("</style></head><body>");

            for (IBodyElement element : document.getBodyElements()) {
                if (element.getElementType() == BodyElementType.PARAGRAPH) {
                    appendParagraph(html, (XWPFParagraph) element);
                } else if (element.getElementType() == BodyElementType.TABLE) {
                    appendTable(html, (XWPFTable) element);
                }
            }

            html.append("</body></html>");
            return new DocxHtmlPreview(html.toString(), false);
        } catch (Exception ex) {
            String fallback = "<!doctype html><html><body><div class=\"fallback\">"
                    + "Không thể hiển thị đầy đủ định dạng, đang hiển thị dạng văn bản thuần."
                    + "</div><pre>" + escape(readPreview(path)) + "</pre></body></html>";
            return new DocxHtmlPreview(fallback, true);
        }
    }

    private String readXlsx(Path path) throws IOException {
        StringBuilder builder = new StringBuilder();
        for (List<String> row : readXlsxRows(path)) {
            builder.append(String.join("\t", row)).append(System.lineSeparator());
        }
        return builder.toString();
    }

    public List<List<String>> readXlsxRows(Path path) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        for (SheetPreview sheet : readXlsxSheets(path)) {
            rows.add(List.of("Trang tính: " + sheet.name()));
            rows.addAll(sheet.rows());
            rows.add(List.of(""));
        }
        return rows;
    }

    public List<SheetPreview> readXlsxSheets(Path path) throws IOException {
        List<SheetPreview> sheets = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (InputStream inputStream = Files.newInputStream(path);
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            for (Sheet sheet : workbook) {
                List<List<String>> rows = new ArrayList<>();
                for (Row row : sheet) {
                    List<String> cells = new ArrayList<>();
                    short lastCell = row.getLastCellNum();
                    for (int i = 0; i < Math.max(lastCell, 0); i++) {
                        Cell cell = row.getCell(i);
                        cells.add(cell == null ? "" : formatter.formatCellValue(cell));
                    }
                    rows.add(cells);
                }
                sheets.add(new SheetPreview(sheet.getSheetName(), rows));
            }
        }
        return sheets;
    }

    private Image toFxImage(BufferedImage bufferedImage) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", outputStream);
        return new Image(new ByteArrayInputStream(outputStream.toByteArray()));
    }

    private void appendParagraph(StringBuilder html, XWPFParagraph paragraph) {
        String tag = resolveParagraphTag(paragraph);
        String listPrefix = paragraph.getNumID() == null ? "" : "<ul><li>";
        String listSuffix = paragraph.getNumID() == null ? "" : "</li></ul>";
        html.append(listPrefix).append('<').append(tag).append(paragraphStyle(paragraph)).append('>');
        for (XWPFRun run : paragraph.getRuns()) {
            appendRun(html, run);
        }
        html.append("</").append(tag).append('>').append(listSuffix);
    }

    private void appendRun(StringBuilder html, XWPFRun run) {
        StringBuilder style = new StringBuilder();
        if (run.getFontSize() > 0) {
            style.append("font-size:").append(run.getFontSize()).append("pt;");
        }
        if (run.getFontFamily() != null) {
            style.append("font-family:'").append(escape(run.getFontFamily())).append("';");
        }
        if (run.getColor() != null) {
            style.append("color:#").append(run.getColor()).append(';');
        }
        html.append("<span");
        if (!style.isEmpty()) {
            html.append(" style=\"").append(style).append("\"");
        }
        html.append('>');
        if (run.isBold()) {
            html.append("<strong>");
        }
        if (run.isItalic()) {
            html.append("<em>");
        }
        if (run.getUnderline() != null && run.getUnderline().getValue() > 0) {
            html.append("<u>");
        }
        html.append(escape(run.text()).replace("\n", "<br>"));
        if (run.getUnderline() != null && run.getUnderline().getValue() > 0) {
            html.append("</u>");
        }
        if (run.isItalic()) {
            html.append("</em>");
        }
        if (run.isBold()) {
            html.append("</strong>");
        }
        html.append("</span>");
    }

    private void appendTable(StringBuilder html, XWPFTable table) {
        html.append("<table>");
        for (XWPFTableRow row : table.getRows()) {
            html.append("<tr>");
            for (XWPFTableCell cell : row.getTableCells()) {
                html.append("<td>");
                for (XWPFParagraph paragraph : cell.getParagraphs()) {
                    appendParagraph(html, paragraph);
                }
                html.append("</td>");
            }
            html.append("</tr>");
        }
        html.append("</table>");
    }

    private String resolveParagraphTag(XWPFParagraph paragraph) {
        String style = paragraph.getStyle();
        if (style == null) {
            return "p";
        }
        String normalized = style.toLowerCase();
        if (normalized.contains("heading1") || normalized.contains("title")) {
            return "h1";
        }
        if (normalized.contains("heading2")) {
            return "h2";
        }
        if (normalized.contains("heading3")) {
            return "h3";
        }
        return "p";
    }

    private String paragraphStyle(XWPFParagraph paragraph) {
        if (paragraph.getAlignment() == null) {
            return "";
        }
        return " style=\"text-align:" + paragraph.getAlignment().name().toLowerCase() + ";\"";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public record DocxHtmlPreview(String html, boolean fallback) {
    }

    public record SheetPreview(String name, List<List<String>> rows) {
    }
}
