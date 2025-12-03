package com.Team4.PFT.Services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.Team4.PFT.DTOs.TxnHistoryDTO;
import com.Team4.PFT.Entities.TxnHistory;
import com.Team4.PFT.Entities.User;
import com.Team4.PFT.Repositories.LoginRepository;
import com.Team4.PFT.Repositories.TxnHistoryRepository;

@Service
public class TxnHistoryService {
	@Autowired
	private TxnHistoryRepository txnHistoryRepository;
	@Autowired
	private LoginRepository loginRepository;
	
	//This is the service for consuming the CSV and addidng to DB.
	@Transactional
    public void uploadCSV(MultipartFile file, Long userId) throws IOException {

        User user = loginRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found by ID"));

        // ✅ NEW: wipe previous transactions for this user
        txnHistoryRepository.deleteByUser(user);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false; // skip header
                    continue;
                }

                String[] fields = line.split(",");

                TxnHistory txn = new TxnHistory();
                txn.setTxnId(UUID.randomUUID());
                txn.setUser(user);

                txn.setTxnDate(LocalDate.parse(fields[1].trim().replace("\"", "")));
                txn.setDescription(fields[2].replace("\"", ""));
                txn.setSubDescription(fields[3].replace("\"", ""));
                txn.setTxnType(fields[4].replace("\"", ""));
                txn.setTxnAmount(Double.parseDouble(fields[5].trim().replace("\"", "")));
                txn.setBalance(Double.parseDouble(fields[6].trim().replace("\"", "")));

                txnHistoryRepository.save(txn);
            }
        }
    }
	
	public List<TxnHistory> searchBetweenDates(LocalDate startDate, LocalDate endDate, Long userId) {
		User user = loginRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found by ID"));
		
		return txnHistoryRepository.findByUserAndTxnDateBetween(user, startDate, endDate);
	}
	
	public double monthsTotal(Long userId) {
		User user = loginRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found by ID"));
		
		LocalDate startDate = LocalDate.now().withDayOfMonth(1);
		LocalDate endDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
		
		List<TxnHistory> txns = txnHistoryRepository.findByUserAndTxnDateBetween(user, startDate, endDate);
		
		return txns.stream()
				.mapToDouble(TxnHistory::getTxnAmount)
				.filter(z -> z < 0)
				.sum();
	}
}
	
	

