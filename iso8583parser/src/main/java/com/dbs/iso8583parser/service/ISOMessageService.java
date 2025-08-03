package com.dbs.iso8583parser.service;
import com.dbs.iso8583parser.model.ISOMessage;
import com.dbs.iso8583parser.repository.ISOMessageRepository;
import com.dbs.iso8583parser.util.AESUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.w3c.dom.*;

        import javax.xml.parsers.*;
        import java.io.*;
        import java.time.*;

@Service
public class ISOMessageService {

    @Autowired
    private ISOMessageRepository repository;

    public ISOMessage parseAndSave(InputStream xmlStream) throws Exception {
        DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = dBuilder.parse(xmlStream);
        doc.getDocumentElement().normalize();

        NodeList fields = doc.getElementsByTagName("field");

        ISOMessage msg = new ISOMessage();

        for (int i = 0; i < fields.getLength(); i++) {
            Element el = (Element) fields.item(i);
            int id = Integer.parseInt(el.getAttribute("id"));
            String value = el.getAttribute("value");
            switch (id) {
                case 0 -> msg.setMti(value);
                case 2 -> msg.setPan(AESUtil.encrypt(value));
                case 3 -> msg.setProcessingCode(value);
                case 4 -> msg.setMontant(Long.parseLong(value));
                case 12 -> msg.setTransactionTime(LocalTime.of(
                        Integer.parseInt(value.substring(0, 2)),
                        Integer.parseInt(value.substring(2, 4)),
                        Integer.parseInt(value.substring(4, 6))));
                case 13 -> msg.setTransactionDate(LocalDate.of(
                        LocalDate.now().getYear(),
                        Integer.parseInt(value.substring(0, 2)),
                        Integer.parseInt(value.substring(2, 4))));
                case 37 -> msg.setRrn(value);
                case 39 -> msg.setResponseCode(value);
                case 41 -> msg.setTerminalId(value);
                case 49 -> msg.setDevise(value);
            }
        }
        return repository.save(msg);
    }
}

