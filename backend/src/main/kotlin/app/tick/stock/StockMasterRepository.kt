package app.tick.stock

import org.springframework.data.jpa.repository.JpaRepository

interface StockMasterRepository : JpaRepository<StockMaster, String>
