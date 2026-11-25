# ifs25058-p6-pbo2026-springboot-cashflow

Aplikasi web pencatat keuangan pribadi (**Cashflow**) berbasis **Spring Boot**, **Maven**, **Vaadin**, dan **JDBC (PostgreSQL)** dengan arsitektur **Clean Architecture** (domain, usecase, adapter, framework) serta **integrasi AI (OpenAI-compatible)** untuk mengubah catatan bebas menjadi transaksi.

| | |
| --- | --- |
| Folder / Artifact Maven | `ifs25058-p6-pbo2026-springboot-cashflow` |
| Package | `org.delcom.ifs25058springbootcashflow` |
| Database | `pbo2026_cashflow` |

## Fitur

- Registrasi, login, logout, profil (nama, password, foto). Role `ADMIN` / `USER`.
- Admin (seed dari `ROOT_USERNAME` / `ROOT_PASSWORD`) dapat melihat daftar pengguna dan transaksi mereka (read-only).
- Transaksi pemasukan/pengeluaran: tambah (dengan bukti gambar opsional), hapus, cari keterangan, filter tipe, 8 opsi urut.
- Kartu ringkasan: total transaksi, total pemasukan, total pengeluaran, saldo (format `Rp 1,000,000.00`).
- **Generate AI**: tulis catatan bebas, pratinjau hasil, lalu simpan.

## Struktur

```
domain/      Transaction, TransactionType, TransactionTypeFilter, SortOption, AiGeneratedTransaction, User, Role, exception, port repository/AI
usecase/     FinanceUseCase, FinanceAiUseCase, AuthUseCase, UserUseCase
adapter/     repository (JDBC + in-memory), ai (OpenAI + in-memory), presenter (FinancePresenter), storage (FileStorage)
framework/   Vaadin views (FinanceView, UserTransactionsView, ...), AuthSession
config/      AppConfig (composition root)
```

## Menjalankan

Prasyarat: JDK 25, PostgreSQL, Maven Wrapper.

```powershell
Copy-Item .env.example .env   # sesuaikan DB_* dan OPENAI_* (API key Gemini dari https://aistudio.google.com/apikey)
.\mvnw.cmd spring-boot:run
```

Buka `http://localhost:8080`. Login admin default: `admin` / `admin` (ubah lewat `.env`).

## Test & coverage

```powershell
.\mvnw.cmd clean verify
```

Jacoco memeriksa coverage 100% (view dan auth dikecualikan) pada fase `verify`. Laporan: `target/site/jacoco/index.html`.

## CI/CD

`Jenkinsfile`: Checkout, Build & Test (Jacoco 100%), Trivy, SonarQube, Quality Gate, Package, Publish, Deploy.
