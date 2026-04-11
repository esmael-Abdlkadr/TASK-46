package com.eaglepoint.workforce.dto;

import jakarta.validation.constraints.NotBlank;

public class ExportRequest {
    @NotBlank(message = "Export name is required")
    private String name;
    @NotBlank(message = "Export type is required")
    private String exportType;
    @NotBlank(message = "File format is required")
    private String fileFormat;
    private String searchCriteria;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getExportType() { return exportType; }
    public void setExportType(String exportType) { this.exportType = exportType; }
    public String getFileFormat() { return fileFormat; }
    public void setFileFormat(String fileFormat) { this.fileFormat = fileFormat; }
    public String getSearchCriteria() { return searchCriteria; }
    public void setSearchCriteria(String searchCriteria) { this.searchCriteria = searchCriteria; }
}
