package com.kaushik.railway.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY createdAt DESC")
    fun getAllBookings(): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE pnr = :pnr LIMIT 1")
    suspend fun getByPnr(pnr: String): BookingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(booking: BookingEntity)

    @Update
    suspend fun update(booking: BookingEntity)

    @Query("UPDATE bookings SET status = :status WHERE pnr = :pnr")
    suspend fun updateStatus(pnr: String, status: String)

    @Query("DELETE FROM bookings WHERE pnr = :pnr")
    suspend fun delete(pnr: String)
}
