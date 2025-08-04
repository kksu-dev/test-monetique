package com.dbs.iso8583parser.controller;
import com.dbs.iso8583parser.model.ISOMessage;
import com.dbs.iso8583parser.repository.ISOMessageRepository;
import com.dbs.iso8583parser.service.ISOMessageService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.dbs.iso8583parser.util.AESUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
@SecurityRequirement(name = "basicAuth")
@RestController
@RequestMapping("/api/messages")
public class ISOMessageController {

    @Autowired
    private ISOMessageRepository repository;

    @Autowired
    private ISOMessageService service;

    //Je construis mon message de retour json
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message, Object data) {
        Map<String, Object> body = new HashMap<>();
        body.put("statusCode", status.value());
        body.put("message", message);
        body.put("data", data);
        return new ResponseEntity<>(body, status);
    }

    //Fonction qui me permet de d'uploader le fichier xml et l'enregistrer dans la BD
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        try {
            ISOMessage saved = service.parseAndSave(file.getInputStream());
            return buildResponse(HttpStatus.OK, "Message ISO sauvegardé avec succès", saved);
        } catch (Exception e) {
            return buildResponse(HttpStatus.BAD_REQUEST, "Erreur lors du traitement du fichier", null);
        }
    }

    //Fonction qui me permet d'obtenir la liste des messages IS 8583 paginées
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        Page<ISOMessage> messages = repository.findAll(PageRequest.of(page, size));

        // Parcours des messages pour décrypter le pan
        messages.getContent().forEach(msg -> {
            if (msg.getPan() != null && !msg.getPan().isEmpty()) {
                try {
                    //Ici je decripte le PAN pour retourner la bonne valeur
                    String decryptedPan = AESUtil.decrypt(msg.getPan());
                    msg.setPan(decryptedPan);
                } catch (Exception e) {
                    msg.setPan("Erreur de déchiffrement");
                }
            }
        });

        return buildResponse(HttpStatus.OK, "Liste des messages ISO", messages);
    }


    //Fonction qui me permet d'obtenir les details du message ISO 8583 par son ID
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOne(@PathVariable Long id) {
        Optional<ISOMessage> msg = repository.findById(id);

        return msg.map(isoMessage -> {
            if (isoMessage.getPan() != null && !isoMessage.getPan().isEmpty()) {
                try {
                    //Ici je decripte le PAN pour retourner la bonne valeur
                    String decryptedPan = AESUtil.decrypt(isoMessage.getPan());
                    isoMessage.setPan(decryptedPan);
                } catch (Exception e) {
                    isoMessage.setPan("Erreur de déchiffrement");
                }
            }
            return buildResponse(HttpStatus.OK, "Détails du message ISO", isoMessage);
        }).orElseGet(() -> buildResponse(HttpStatus.NOT_FOUND, "Message non trouvé", null));
    }


    //Ici je supprime le message ISO 8583 par son ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return buildResponse(HttpStatus.NO_CONTENT, "Message supprimé avec succès", null);
        } else {
            return buildResponse(HttpStatus.NOT_FOUND, "Message non trouvé", null);
        }
    }
}