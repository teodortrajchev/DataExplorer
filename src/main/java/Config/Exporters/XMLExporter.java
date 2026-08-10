package Config.Exporters;


import Config.Database.DataTable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;


public class XMLExporter implements IExporter {

    @Override
    public void export(DataTable table, Path outputFile) throws IOException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement("data");
            root.setAttribute("title", table.getTitle());
            root.setAttribute("rowCount", String.valueOf(table.getRowCount()));
            doc.appendChild(root);

            List<String> columns = table.getColumnNames();

            for (Object[] rowData : table.getRows()) {
                Element rowEl = doc.createElement("row");
                root.appendChild(rowEl);

                for (int i = 0; i < columns.size(); i++) {
                    Element fieldEl = doc.createElement(sanitizeTagName(columns.get(i)));
                    Object value = rowData[i];
                    fieldEl.setTextContent(value == null ? "" : value.toString());
                    rowEl.appendChild(fieldEl);
                }
            }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            transformer.transform(new DOMSource(doc), new StreamResult(outputFile.toFile()));
        } catch (Exception e) {
            throw new IOException("Failed to export XML", e);
        }
    }

    /** XML element names can't start with a digit, contain spaces, etc. */
    private String sanitizeTagName(String name) {
        String cleaned = name.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
        if (cleaned.isEmpty() || Character.isDigit(cleaned.charAt(0))) {
            cleaned = "col_" + cleaned;
        }
        return cleaned;
    }

    @Override
    public String fileExtension() { return "xml"; }
}