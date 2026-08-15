package com.stock.ticker.extraction;

import com.stock.ticker.model.ActiveSymbols;
import com.stock.ticker.model.WorkingCapital;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Service
public class ExtractionService {
    private final NamedParameterJdbcTemplate jdbc;

    public ExtractionService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<WorkingCapital> getDataForWorkingCapital(String symbol, String periodType, int limit){

        return jdbc.query("""
                select bsp.current_net_receivables, bsp.inventory, bsp.current_accounts_payable, bsp.fiscal_date_ending from public.balance_sheet_periods bsp
                where bsp.symbol = :symbol and bsp.period_type = :periodType::period_type order by bsp.fiscal_date_ending desc limit :limit;
                """, new MapSqlParameterSource().addValue("symbol", symbol).addValue("periodType", periodType).addValue("limit", limit), new RowMapper<WorkingCapital>() {
            @Override
            public WorkingCapital mapRow(ResultSet rs, int rowNum) throws SQLException {
                return WorkingCapital.forWorkingCapitalRatio(rs.getLong("inventory"), rs.getLong("current_net_receivables"), rs.getLong("current_accounts_payable"),
                        rs.getString("fiscal_date_ending"));
            }
        } );
    }

    public List<HashMap<String, String>> getTotalRevenue(String symbol, String periodType, int limit){
        List<HashMap<String, String>> dbResult = new ArrayList<>();
        return jdbc.query("""
                SELECT symbol, total_revenue, fiscal_date_ending FROM public.income_statement_periods
                where symbol = :symbol and period_type = :periodType::period_type order by fiscal_date_ending desc limit :limit;
                """, new MapSqlParameterSource().addValue("symbol", symbol).addValue("periodType", periodType).addValue("limit", limit) ,new RowMapper<HashMap<String, String>>() {
            @Override
            public HashMap<String, String> mapRow(ResultSet rs, int rowNum) throws SQLException {
                HashMap<String, String> dbRow = new HashMap<>();
                dbRow.put("symbol", rs.getString("symbol"));
                dbRow.put("totalRevenue", rs.getString("total_revenue"));
                dbRow.put("fiscalDateEnding", rs.getString("fiscal_date_ending"));
                return dbRow;
            }
        } );
    }



    public HashMap<String, Boolean> getActiveSymbols(){
        List<ActiveSymbols> dbResult = jdbc.query("""
                SELECT symbol,active FROM public.symbols where active = true;
                """, new RowMapper<ActiveSymbols>() {
            @Override
            public ActiveSymbols mapRow(ResultSet rs, int rowNum) throws SQLException {
                return ActiveSymbols.getActiveSymbol(rs.getString("symbol"), rs.getBoolean("active")) ;
            }
        });

        HashMap<String, Boolean> symbolsMap = new HashMap<>();
        for(ActiveSymbols activeSymbols: dbResult)
            symbolsMap.put(activeSymbols.symbol(), activeSymbols.active());

        return symbolsMap;
    }


}
