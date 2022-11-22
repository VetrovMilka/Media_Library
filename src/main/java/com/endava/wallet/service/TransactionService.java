package com.endava.wallet.service;

import com.endava.wallet.entity.Profile;
import com.endava.wallet.entity.Transaction;
import com.endava.wallet.entity.User;
import com.endava.wallet.exception.ApiRequestException;
import com.endava.wallet.repository.TransactionRepository;
import com.endava.wallet.repository.TransactionsCategoryRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class TransactionService {

    private TransactionRepository transactionRepository;
    private TransactionsCategoryRepository categoryRepository;
    private ProfileService profileService;


    public List<Transaction> findRecentTransactionsByUser(User user) {
        Profile profile = profileService.findProfileByUser(user);
        return findRecentTransactionsByProfile(profile);
    }

    public List<Transaction> findRecentTransactionsByProfile(Profile profile) {
        List<Transaction> transactions = transactionRepository.findTransactionByProfileOrderByIdAsc(profile);
        Collections.reverse(transactions);
        return transactions;
    }

    public BigDecimal findTranSumDateBetween(Profile profile, boolean isIncome, LocalDate from, LocalDate to) {
        List<Transaction> transactions = transactionRepository.findByProfileAndIsIncomeAndTransactionDateBetween(
                profile,
                isIncome,
                from,
                to);

        return transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Pair<String, BigDecimal> findMaxCategorySumDateBetween(Profile profile, boolean isIncome, LocalDate from, LocalDate to) {
        String maxTranCategory = transactionRepository.findMaxCategoryDateBetween(
                profile,
                isIncome,
                from,
                to
        );
        if (maxTranCategory == null) maxTranCategory = "nothing";

        BigDecimal maxTranSum = transactionRepository.findMaxSumDateBetween(
                profile,
                isIncome,
                from,
                to
        );
        if (maxTranSum == null) maxTranSum = BigDecimal.ZERO;

        return Pair.of(maxTranCategory, maxTranSum);
    }

    public Transaction findTransactionById(Long id) {
        if (transactionRepository.findTransactionById(id) == null) {
            throw new ApiRequestException("Transaction with id: " + id + " not found");
        }
        return transactionRepository.findTransactionById(id);
    }
    public Transaction findTransactionByIdAndProfile(Long id, Profile profile) {
        if (transactionRepository.findTransactionById(id).getProfile() != profile) {
            throw new ApiRequestException("Transaction with id: " + id + " not found");
        }
        return transactionRepository.findTransactionById(id);
    }

    public void add(Transaction transaction, Profile profile) {
        transactionRepository.save(transaction);

        if (Boolean.TRUE.equals(transaction.getIsIncome())) {
            profile.setBalance(profile.getBalance().add(transaction.getAmount()));
        } else {
            profile.setBalance(profile.getBalance().subtract(transaction.getAmount()));
        }
        profileService.save(profile);
    }

    public void save(Transaction transaction) {
        transactionRepository.save(transaction);
    }

    public void save(User user, Long id, String message, String category, BigDecimal amount, String transactionDate) {
        Profile profile = profileService.findProfileByUser(user);
        Transaction transaction = findTransactionByIdAndProfile(id, profile);
        if (amount != null && !amount.equals(transaction.getAmount())) {
            if (Boolean.TRUE.equals(transaction.getIsIncome())) {
                profile.setBalance(profile.getBalance().subtract(transaction.getAmount()));
                profile.setBalance(profile.getBalance().add(amount));
            } else {
                profile.setBalance(profile.getBalance().add(transaction.getAmount()));
                profile.setBalance(profile.getBalance().subtract(amount));
            }
        }

        if (amount != null) {
            transaction.setAmount(amount);
        }
        transaction.setCategory(categoryRepository.findByCategory(category));
        transaction.setTransactionDate(parseDate(transactionDate));
        transaction.setMessage(message);

        transactionRepository.save(transaction);
        profileService.save(profile);
    }

    public LocalDate parseDate(String transactionDate) {
        return LocalDate.parse(transactionDate);
    }

    public void deleteTransactionById(Long transactionID, User user) {
        Profile profile = profileService.findProfileByUser(user);
        findTransactionByIdAndProfile(transactionID, profile);
        Transaction transaction = transactionRepository.findTransactionById(transactionID);
        transactionRepository.deleteById(transactionID);
        if (Boolean.TRUE.equals(transaction.getIsIncome())) {
            profile.setBalance(profile.getBalance().subtract(transaction.getAmount()));
        } else {
            profile.setBalance(profile.getBalance().add(transaction.getAmount()));
        }
        profileService.save(profile);

    }
}
