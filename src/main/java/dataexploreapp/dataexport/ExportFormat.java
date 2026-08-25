package dataexploreapp.dataexport;

import dataexploreapp.dataexport.exporters.*;

public enum ExportFormat {

    XLSX(".xlsx", new ExcelExporter()),
    CSV(".csv", new CSVExporter()),
    XML(".xml", new XMLExporter()),
    ODX(".odx", new WordExporter()),
    PDF(".pdf", new PDFExporter()),
    JSON(".json",new JSONExporter());
    private final String extension;
    private final IExporter exporter;

    ExportFormat(
            String extension,
            IExporter exporter
    ) {
        this.extension = extension;
        this.exporter = exporter;
    }

    public String getExtension() {
        return extension;
    }

    public IExporter getExporter() {
        return exporter;
    }

    @Override
    public String toString() {
        return switch (this) {
            case XLSX -> "Excel (.xlsx)";
            case CSV -> "CSV (.csv)";
            case XML -> "XML (.xml)";
            case ODX -> "ODX (.odx)";
            case PDF -> "PDF (.pdf)";
            case JSON -> "JSON (.json)";
        };
    }
}