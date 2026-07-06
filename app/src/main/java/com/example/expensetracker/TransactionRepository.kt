package com.example.expensetracker

import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val merchantMappingDao: MerchantCategoryMappingDao
) {

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val totalExpenses: Flow<Int?> = transactionDao.getTotalExpenses()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    val allMerchantMappings: Flow<List<MerchantCategoryMapping>> =
        merchantMappingDao.getAllMappings()

    fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    fun getTransactionById(id: Int): Transaction? {
        return transactionDao.getTransactionById(id)
    }

    fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    fun getTransactionsByCategory(category: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByCategory(category)
    }

    fun getTotalByPaymentMethod(method: String): Flow<Int?> {
        return transactionDao.getTotalByPaymentMethod(method)
    }

    fun insertCategory(category: Category) {
        categoryDao.insertCategory(category)
    }

    fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }

    fun getDefaultCategories(): Flow<List<Category>> {
        return categoryDao.getDefaultCategories()
    }

    fun getCustomCategories(): Flow<List<Category>> {
        return categoryDao.getCustomCategories()
    }

    fun getCategoryIdByKeyword(keyword: String): Int? {
        return merchantMappingDao.getCategoryIdByKeyword(keyword)
    }

    fun getConfidenceByKeyword(keyword: String): Int? {
        return merchantMappingDao.getConfidenceByKeyword(keyword)
    }

    fun insertMerchantMapping(mapping: MerchantCategoryMapping) {
        merchantMappingDao.insertMapping(mapping)
    }

    fun updateMerchantMapping(mapping: MerchantCategoryMapping) {
        merchantMappingDao.updateMapping(mapping)
    }

    fun getCategorySpending(): Flow<List<CategorySpending>> {
        return transactionDao.getCategorySpending()
    }

    fun getDailySpending(): Flow<List<DailySpending>> {
        return transactionDao.getDailySpending()
    }

    fun getMonthlySpending(): Flow<List<MonthlySpending>> {
        return transactionDao.getMonthlySpending()
    }
}