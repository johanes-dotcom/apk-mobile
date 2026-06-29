package com.example.tts_pemogramanmobile

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.NumberFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var typeSpinner: Spinner
    private lateinit var amountEditText: EditText
    private lateinit var notesEditText: EditText
    private lateinit var monthSpinner: Spinner
    private lateinit var yearEditText: EditText
    private lateinit var totalIncomeTextView: TextView
    private lateinit var totalExpenseTextView: TextView
    private lateinit var balanceTextView: TextView
    private lateinit var showReportButton: Button
    private lateinit var saveButton: Button
    private lateinit var transactionListLinearLayout: LinearLayout
    private lateinit var emptyTransactionTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        databaseHelper = DatabaseHelper(this)

        connectView()
        setupSpinnerAdapter()
        setCurrentPeriod()
        setupButton()
        
        // Tampilkan laporan saat aplikasi dibuka
        showReport()
    }

    private fun connectView() {
        typeSpinner = findViewById(R.id.typeSpinner)
        amountEditText = findViewById(R.id.amountEditText)
        notesEditText = findViewById(R.id.notesEditText)
        monthSpinner = findViewById(R.id.monthSpinner)
        yearEditText = findViewById(R.id.yearEditText)
        totalIncomeTextView = findViewById(R.id.totalIncomeTextView)
        totalExpenseTextView = findViewById(R.id.totalExpenseTextView)
        balanceTextView = findViewById(R.id.BalanceTextView)
        showReportButton = findViewById(R.id.showReportButton)
        saveButton = findViewById(R.id.saveButton)
        transactionListLinearLayout = findViewById(R.id.transactionListLinearLayout)
        emptyTransactionTextView = findViewById(R.id.emptyTransactionTextView)
    }

    private fun setupSpinnerAdapter() {
        val transactionType = listOf("PENDAPATAN", "PENGELUARAN")
        val typeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            transactionType
        )
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = typeAdapter

        val months = listOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
        val monthAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            months
        )
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        monthSpinner.adapter = monthAdapter
    }

    private fun setCurrentPeriod() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        monthSpinner.setSelection(currentMonth)
        yearEditText.setText(currentYear.toString())
    }

    private fun setupButton() {
        saveButton.setOnClickListener {
            saveTransaction()
        }
        showReportButton.setOnClickListener {
            showReport()
        }
    }

    private fun saveTransaction() {
        val amountText = amountEditText.text.toString()
        if (amountText.isEmpty()) {
            amountEditText.error = "Masukan nilai"
            return
        }
        val amount = amountText.toLongOrNull()
        if (amount == null) {
            amountEditText.error = "Masukan nilai yang valid"
            amountEditText.requestFocus()
            return
        }

        val note = notesEditText.text.toString()
        if (note.isEmpty()) {
            notesEditText.error = "Masukan keterangan"
            notesEditText.requestFocus()
            return
        }

        val yearText = yearEditText.text.toString()
        if (yearText.isEmpty() || yearText.length != 4) {
            yearEditText.error = "Masukan tahun yang valid"
            yearEditText.requestFocus()
            return
        }
        val year = yearText.toIntOrNull()
        if (year == null || year < 2000) {
            yearEditText.error = "Masukan tahun yang valid"
            yearEditText.requestFocus()
            return
        }

        val type = typeSpinner.selectedItem.toString()
        val month = monthSpinner.selectedItemPosition + 1

        val isSuccessful = databaseHelper.insertTransaction(type, amount, note, month, year)
        if (isSuccessful) {
            Toast.makeText(this, "Transaksi berhasil disimpan", Toast.LENGTH_SHORT).show()
            showReport()
            clearForm()
        } else {
            Toast.makeText(this, "Transaksi gagal disimpan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearForm() {
        typeSpinner.setSelection(0)
        amountEditText.text.clear()
        notesEditText.text.clear()
        setCurrentPeriod()
    }

    private fun showReport() {
        // Mengambil laporan keseluruhan tanpa filter agar semua muncul
        val report = databaseHelper.getAllReport()
        val totalIncome = report.first
        val totalExpense = report.second
        val balance = totalIncome - totalExpense
        
        totalIncomeTextView.text = "Total pendapatan: ${formatMoney(totalIncome)}"
        totalExpenseTextView.text = "Total pengeluaran: ${formatMoney(totalExpense)}"
        balanceTextView.text = "Saldo akhir: ${formatMoney(balance)}"
        
        loadTransactionList()
    }

    private fun formatMoney(amount: Long): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return formatter.format(amount)
    }

    private fun loadTransactionList() {
        transactionListLinearLayout.removeAllViews()
        val transactions = databaseHelper.getAllTransactions()

        if (transactions.isEmpty()) {
            emptyTransactionTextView.visibility = View.VISIBLE
            transactionListLinearLayout.visibility = View.GONE
            return
        }
        emptyTransactionTextView.visibility = View.GONE
        transactionListLinearLayout.visibility = View.VISIBLE

        for (transaction in transactions) {
            val transactionItem = layoutInflater.inflate(
                R.layout.item_detail,
                transactionListLinearLayout,
                false
            )

            val typeTextView = transactionItem.findViewById<TextView>(R.id.typeItemTextView)
            val amountTextView = transactionItem.findViewById<TextView>(R.id.amountItemTextView)
            val noteTextView = transactionItem.findViewById<TextView>(R.id.noteItemTextView)
            val deleteButton = transactionItem.findViewById<Button>(R.id.deleteItemButton)

            typeTextView.text = transaction.type
            amountTextView.text = formatMoney(transaction.amount)
            noteTextView.text = transaction.note
            
            deleteButton?.setOnClickListener {
                deleteTransaction(transaction.id)
            }

            transactionListLinearLayout.addView(transactionItem)
        }
    }

    private fun deleteTransaction(id: Long) {
        val isDeleted = databaseHelper.deleteTransaction(id)
        if (isDeleted) {
            Toast.makeText(this, "Transaksi berhasil dihapus", Toast.LENGTH_SHORT).show()
            showReport()
        } else {
            Toast.makeText(this, "Transaksi gagal dihapus", Toast.LENGTH_SHORT).show()
        }
    }
}

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "finance.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_TRANSACTIONS = "transactions"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TYPE = "type"
        private const val COLUMN_AMOUNT = "amount"
        private const val COLUMN_NOTE = "note"
        private const val COLUMN_MONTH = "month"
        private const val COLUMN_YEAR = "year"
    }

    data class FinanceTransaction(
        val id: Long,
        val type: String,
        val amount: Long,
        val note: String
    )

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_TRANSACTIONS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TYPE TEXT NOT NULL,
                $COLUMN_AMOUNT INTEGER NOT NULL,
                $COLUMN_NOTE TEXT NOT NULL,
                $COLUMN_MONTH INTEGER NOT NULL,
                $COLUMN_YEAR INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TRANSACTIONS")
        onCreate(db)
    }

    fun insertTransaction(type: String, amount: Long, note: String, month: Int, year: Int): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TYPE, type)
            put(COLUMN_AMOUNT, amount)
            put(COLUMN_NOTE, note)
            put(COLUMN_MONTH, month)
            put(COLUMN_YEAR, year)
        }
        val result = db.insert(TABLE_TRANSACTIONS, null, values)
        return result != -1L
    }

    fun getAllReport(): Pair<Long, Long> {
        var totalIncome = 0L
        var totalExpense = 0L
        val query = """
            SELECT 
                COALESCE(SUM(CASE WHEN $COLUMN_TYPE = 'PENDAPATAN' THEN $COLUMN_AMOUNT ELSE 0 END), 0) AS total_income,
                COALESCE(SUM(CASE WHEN $COLUMN_TYPE = 'PENGELUARAN' THEN $COLUMN_AMOUNT ELSE 0 END), 0) AS total_expense
            FROM $TABLE_TRANSACTIONS
        """.trimIndent()
        val db = readableDatabase
        db.rawQuery(query, null).use { cursor ->
            if (cursor.moveToFirst()) {
                totalIncome = cursor.getLong(0)
                totalExpense = cursor.getLong(1)
            }
        }
        return Pair(totalIncome, totalExpense)
    }

    fun getAllTransactions(): List<FinanceTransaction> {
        val transactions = mutableListOf<FinanceTransaction>()
        val query = "SELECT $COLUMN_ID, $COLUMN_TYPE, $COLUMN_AMOUNT, $COLUMN_NOTE FROM $TABLE_TRANSACTIONS ORDER BY $COLUMN_ID DESC"
        val db = readableDatabase
        db.rawQuery(query, null).use { cursor ->
            while (cursor.moveToNext()) {
                transactions.add(FinanceTransaction(
                    id = cursor.getLong(0),
                    type = cursor.getString(1),
                    amount = cursor.getLong(2),
                    note = cursor.getString(3)
                ))
            }
        }
        return transactions
    }

    fun deleteTransaction(id: Long): Boolean {
        val db = writableDatabase
        val deletedRows = db.delete(TABLE_TRANSACTIONS, "$COLUMN_ID = ?", arrayOf(id.toString()))
        return deletedRows > 0
    }
}
