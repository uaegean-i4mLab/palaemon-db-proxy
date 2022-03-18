package gr.uaegean.palaemondbproxy.controllers;

import gr.uaegean.palaemondbproxy.model.Geofence;
import gr.uaegean.palaemondbproxy.model.PameasPerson;
import gr.uaegean.palaemondbproxy.model.Personalinfo;
import gr.uaegean.palaemondbproxy.model.TO.*;
import gr.uaegean.palaemondbproxy.model.TicketInfo;
import gr.uaegean.palaemondbproxy.model.location.UserGeofenceUnit;
import gr.uaegean.palaemondbproxy.model.location.UserLocationUnit;
import gr.uaegean.palaemondbproxy.repository.GeofenceRepository;
import gr.uaegean.palaemondbproxy.service.ElasticService;
import gr.uaegean.palaemondbproxy.service.KafkaService;
import gr.uaegean.palaemondbproxy.utils.CryptoUtils;
import gr.uaegean.palaemondbproxy.utils.PameasPersonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@RestController
@Slf4j
public class PersonControllers {

    @Autowired
    KafkaService kafkaService;

    @Autowired
    CryptoUtils cryptoUtils;

    @Autowired
    ElasticService elasticService;

    @Autowired
    GeofenceRepository geofenceRepository;

    @PostMapping("/addPerson")
    public void addPerson(@RequestBody PersonTO person) {
        try {
            person.setName(cryptoUtils.encryptBase64(person.getName()));
            person.setSurname(cryptoUtils.encryptBase64(person.getSurname()));
            person.setIdentifier(cryptoUtils.encryptBase64(person.getIdentifier()));
            if (person.getConnectedPassengers() != null) {
                person.getConnectedPassengers().forEach(connectedPersonTO -> {
                    try {
                        connectedPersonTO.setName(cryptoUtils.encryptBase64(connectedPersonTO.getName()));
                        connectedPersonTO.setSurname(cryptoUtils.encryptBase64(connectedPersonTO.getSurname()));

                    } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
                        log.error("Error in encrypting person ");
                        log.error(e.getMessage());
                    }
                });
            }


        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            log.error("Error in encrypting person ");
            log.error(e.getMessage());
        }
        PameasPerson wrappedPerson = PameasPersonFactory.getFromPersonTO(person);
//        log.info(wrappedPerson.toString());
        kafkaService.savePerson(wrappedPerson);
//        elasticService.save(wrappedPerson);

    }

    @GetMapping("/getPerson")
    public PameasPerson getPersonalInfo(@RequestParam String id) {
        Optional<PameasPerson> pameasPerson = Optional.empty();
        //            String encryptedId = this.cryptoUtils.encryptBase64(id);
//            log.info(encryptedId);
//            log.info(this.cryptoUtils.decryptBase64Message(encryptedId));
        pameasPerson = elasticService.getPersonByPersonalIdentifierDecrypted(id);
        if (pameasPerson.isPresent()) {
            Personalinfo personalinfo = pameasPerson.get().getPersonalInfo();
            List<TicketInfo> ticketInfoList = pameasPerson.get().getPersonalInfo().getTicketInfo();

            try {
                personalinfo.setPersonalId(cryptoUtils.decryptBase64Message(personalinfo.getPersonalId()));
                personalinfo.setName(cryptoUtils.decryptBase64Message(personalinfo.getName()));
                personalinfo.setSurname(cryptoUtils.decryptBase64Message(personalinfo.getSurname()));
                pameasPerson.get().setPersonalInfo(personalinfo);
                pameasPerson.get().getPersonalInfo().setTicketInfo(ticketInfoList.stream().map(ticketInfo -> {
                    try {
                        ticketInfo.setName(cryptoUtils.decryptBase64Message(ticketInfo.getName()));
                        ticketInfo.setSurname(cryptoUtils.decryptBase64Message(ticketInfo.getSurname()));
                        return ticketInfo;
                    } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
                        log.error("ERROR decrypting TicketInfo details");
                        log.error(e.getMessage());
                        return null;
                    }
                }).collect(Collectors.toList()));

            } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
                log.error("ERROR decrypting person details");
                log.error(e.getMessage());
            }
        }
        return pameasPerson.orElse(null);
    }

    @GetMapping("/getAll")
    public @ResponseBody
    List<PameasPerson> getPersonalInfo() {
        return elasticService.getAllPersonsDecrypted();
    }

    @GetMapping("/getPassengers")
    public @ResponseBody
    List<PameasPerson> getAllPassengerInfo() {
        return elasticService.getAllPersonsDecrypted().stream()
                .filter(pameasPerson -> pameasPerson.getPersonalInfo().getRole().equals("passenger")).collect(Collectors.toList());
    }


    @GetMapping("/getPersonPosition")
    public @ResponseBody
    LatestLocationTO getPersonPosition(@RequestParam String id) {
        LatestLocationTO result = new LatestLocationTO();
        Optional<PameasPerson> pameasPerson = Optional.empty();
        pameasPerson = elasticService.getPersonByPersonalIdentifierDecrypted(id);

        if (pameasPerson.isPresent()) {
            UserLocationUnit latestLocation =
                    pameasPerson.get().getLocationInfo().getLocationHistory().get(pameasPerson.get().getLocationInfo().getLocationHistory().size() - 1);
            UserGeofenceUnit latestGeofenceUnit =
                    pameasPerson.get().getLocationInfo().getGeofenceHistory().get(pameasPerson.get().getLocationInfo().getGeofenceHistory().size() - 1);
            Optional<Geofence> latestGeofence = geofenceRepository.findById(latestGeofenceUnit.getGfId());
            if (latestGeofence.isPresent()) {
                result.setGeofence(latestGeofence.get());
                result.setLocation(latestLocation);
            }
        }
        return result;
    }


    @GetMapping("/getPassengerCommunicationDetails")
    public @ResponseBody
    List<PassengerCommunicationDetailsTO> getPassengerCommunicationDetails() {

        return elasticService.getAllPersonsDecrypted().stream()
                .filter(pameasPerson -> pameasPerson.getPersonalInfo().getRole().equals("passenger"))
                .map(pameasPerson -> {
                    PassengerCommunicationDetailsTO detailsTO = new PassengerCommunicationDetailsTO();
                    detailsTO.setMacAddress(pameasPerson.getNetworkInfo().getDeviceInfoList().get(0).getMacAddress());
                    String[] preferredLanguages = new String[pameasPerson.getPersonalInfo().getPreferredLanguage().size()];
                    detailsTO.setPreferredLanguage(pameasPerson.getPersonalInfo().getPreferredLanguage().toArray(preferredLanguages));
                    return detailsTO;
                }).collect(Collectors.toList());
    }


    @GetMapping("/getPassengerMusteringDetails")
    public @ResponseBody
    List<PassengerMusteringDetailsTO> getPassengerMusteringDetails() {

        return elasticService.getAllPersonsDecrypted().stream()
                .filter(pameasPerson -> pameasPerson.getPersonalInfo().getRole().equals("passenger"))
                .map(pameasPerson -> {
                    PassengerMusteringDetailsTO detailsTO = new PassengerMusteringDetailsTO();
                    detailsTO.setGeneralInfo(PameasPersonFactory.transformPameasPerson2PersonTO(pameasPerson));
                    detailsTO.setCommunicationDetails(transformPameasPerson2PassengerCommunicationDetailsTO(pameasPerson));
                    detailsTO.setLastKnowLocation(transformPameasPerson2LatestLocation(pameasPerson));
                    return detailsTO;
                })
                .collect(Collectors.toList());
    }


    @PostMapping("/updatePassengerMS")
    public @ResponseBody
    String updatePassengerMS(@RequestBody UpdatePersonStatusTO person) {
        Optional<PameasPerson> existingPerson = elasticService.getPersonByPersonalIdentifierDecrypted(person.getId());
        existingPerson.ifPresent(pameasPerson -> pameasPerson.getPersonalInfo().setAssignedMusteringStation(person.getMusteringStation()));
        existingPerson.ifPresent(pameasPerson -> elasticService.save(pameasPerson));
        return "OK";
    }


    public LatestLocationTO transformPameasPerson2LatestLocation(PameasPerson pameasPerson) {
        LatestLocationTO result = new LatestLocationTO();

        UserLocationUnit latestLocation =
                pameasPerson.getLocationInfo().getLocationHistory().get(pameasPerson.getLocationInfo().getLocationHistory().size() - 1);
        UserGeofenceUnit latestGeofenceUnit =
                pameasPerson.getLocationInfo().getGeofenceHistory().get(pameasPerson.getLocationInfo().getGeofenceHistory().size() - 1);
        Optional<Geofence> latestGeofence = geofenceRepository.findById(latestGeofenceUnit.getGfId());
        if (latestGeofence.isPresent()) {
            result.setGeofence(latestGeofence.get());
            result.setLocation(latestLocation);
        }
        return result;
    }


    public PassengerCommunicationDetailsTO transformPameasPerson2PassengerCommunicationDetailsTO(PameasPerson pameasPerson) {
        String[] preferredLanguages = new String[pameasPerson.getPersonalInfo().getPreferredLanguage().size()];
        PassengerCommunicationDetailsTO communicationDetailsTO = new PassengerCommunicationDetailsTO();
        communicationDetailsTO.setMacAddress(pameasPerson.getNetworkInfo().getDeviceInfoList().get(0).getMacAddress());
        communicationDetailsTO.setPreferredLanguage(pameasPerson.getPersonalInfo().getPreferredLanguage().toArray(preferredLanguages));
        return communicationDetailsTO;
    }

}