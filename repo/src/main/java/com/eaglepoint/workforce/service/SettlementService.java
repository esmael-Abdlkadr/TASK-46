package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.PaymentTransaction;
import com.eaglepoint.workforce.repository.PaymentTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class SettlementService {

    private final PaymentTransactionRepository paymentRepo;

    public SettlementService(PaymentTransactionRepository paymentRepo) {
        this.paymentRepo = paymentRepo;
    }

    @Transactional(readOnly = true)
    public String generateMonthlySettlementCsv(String location, YearMonth yearMonth) {
        LocalDateTime from = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime to = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        List<PaymentTransaction> payments = paymentRepo.findByLocationAndTransactionDateBetween(
                location, from, to);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.println("Settlement Statement");
        pw.println("Location: " + location);
        pw.println("Period: " + yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        pw.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        pw.println();
        pw.println("Reference,Date,Channel,Payer,Amount,Refunded,Net,Status");

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalRefunded = BigDecimal.ZERO;

        for (PaymentTransaction p : payments) {
            pw.printf("%s,%s,%s,%s,%s,%s,%s,%s%n",
                    escapeCsv(p.getReferenceNumber()),
                    p.getTransactionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    p.getChannel().getDisplayName(),
                    escapeCsv(p.getPayerName() != null ? p.getPayerName() : ""),
                    p.getAmount(),
                    p.getRefundedAmount(),
                    p.getNetAmount(),
                    p.getStatus().getDisplayName());

            totalAmount = totalAmount.add(p.getAmount());
            totalRefunded = totalRefunded.add(p.getRefundedAmount());
        }

        pw.println();
        pw.printf("TOTALS,,,,,%s,%s,%s%n", totalAmount, totalRefunded,
                totalAmount.subtract(totalRefunded));
        pw.printf("Transaction Count: %d%n", payments.size());

        return sw.toString();
    }

    @Transactional(readOnly = true)
    public List<String> getLocations() {
        return paymentRepo.findDistinctLocations();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
