package org.delcom.ifs25058springbootcashflow.domain.repository;

import org.delcom.ifs25058springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25058springbootcashflow.domain.exception.DomainException;

import java.util.List;

/**
 * Port layanan AI untuk menghasilkan transaksi dari catatan keuangan bebas.
 */
public interface IFinanceAiService {

    /**
     * Menghasilkan daftar transaksi dari prompt teks bebas.
     *
     * @param prompt catatan/instruksi pengguna
     * @return daftar transaksi hasil generasi
     * @throws DomainException jika komunikasi AI atau parsing gagal
     */
    List<AiGeneratedTransaction> generateTransactions(String prompt) throws DomainException;
}
