package com.dbs.iso8583parser.repository;
import com.dbs.iso8583parser.model.ISOMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ISOMessageRepository extends JpaRepository<ISOMessage, Long> {
}