package com.lz.radar.common;

import lombok.Data;

import java.util.List;

/**
 * 表格数据信息
 */
@Data
public class TableDataInfo<T> {
    
    /**
     * 总记录数
     */
    private long total;
    
    /**
     * 列表数据
     */
    private List<T> rows;
    
    /**
     * 消息状态码
     */
    private int code;
    
    /**
     * 消息内容
     */
    private String msg;
    
    public TableDataInfo() {
    }
    
    public TableDataInfo(long total, List<T> rows) {
        this.total = total;
        this.rows = rows;
        this.code = 200;
        this.msg = "操作成功";
    }
    
    /**
     * 构建分页表格数据
     */
    public static <T> TableDataInfo<T> build(long total, List<T> rows) {
        return new TableDataInfo<>(total, rows);
    }
    
    /**
     * 构建不分页的表格数据（查询所有）
     */
    public static <T> TableDataInfo<T> getDataTable(List<T> rows) {
        TableDataInfo<T> tableDataInfo = new TableDataInfo<>();
        tableDataInfo.setRows(rows);
        tableDataInfo.setTotal(rows.size());
        tableDataInfo.setCode(200);
        tableDataInfo.setMsg("操作成功");
        return tableDataInfo;
    }
    
    /**
     * 构建分页表格数据
     */
    public static <T> TableDataInfo<T> getDataTable(List<T> rows, long total) {
        TableDataInfo<T> tableDataInfo = new TableDataInfo<>();
        tableDataInfo.setRows(rows);
        tableDataInfo.setTotal(total);
        tableDataInfo.setCode(200);
        tableDataInfo.setMsg("操作成功");
        return tableDataInfo;
    }
    
    /**
     * 构建错误信息
     */
    public static <T> TableDataInfo<T> error(String msg) {
        TableDataInfo<T> tableDataInfo = new TableDataInfo<>();
        tableDataInfo.setCode(500);
        tableDataInfo.setMsg(msg);
        return tableDataInfo;
    }
    
    /**
     * 构建错误信息
     */
    public static <T> TableDataInfo<T> error(int code, String msg) {
        TableDataInfo<T> tableDataInfo = new TableDataInfo<>();
        tableDataInfo.setCode(code);
        tableDataInfo.setMsg(msg);
        return tableDataInfo;
    }
}