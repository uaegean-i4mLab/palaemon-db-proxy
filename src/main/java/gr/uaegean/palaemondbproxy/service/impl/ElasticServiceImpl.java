package gr.uaegean.palaemondbproxy.service.impl;

import gr.uaegean.palaemondbproxy.model.EvacuationStatus;
import gr.uaegean.palaemondbproxy.model.Geofence;
import gr.uaegean.palaemondbproxy.model.PameasPerson;
import gr.uaegean.palaemondbproxy.repository.EvacuationStatusRepository;
import gr.uaegean.palaemondbproxy.repository.PameasPersonRepository;
import gr.uaegean.palaemondbproxy.service.ElasticService;
import gr.uaegean.palaemondbproxy.utils.CryptoUtils;
import gr.uaegean.palaemondbproxy.utils.PameasPersonUtils;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.matchQuery;

@Service
@Slf4j
public class ElasticServiceImpl implements ElasticService {

    @Autowired
    ElasticsearchOperations elasticsearchTemplate;

    @Autowired
    PameasPersonRepository personRepository;

    @Autowired
    EvacuationStatusRepository evacuationStatusRepository;

    @Autowired
    CryptoUtils cryptoUtils;


    // personalIdentifier in plain text,
    @Override
    public Optional<PameasPerson> getPersonByPersonalIdentifierDecrypted(String personalIdentifier) {
        String date = DateTimeFormatter.ofPattern("yyyy.MM.dd").format(LocalDate.now());

        Query searchQuery = new NativeSearchQueryBuilder()
//                .withQuery(matchQuery("personalInfo.personalId",personalIdentifier).minimumShouldMatch("100%"))
                .build();
        List<SearchHit<PameasPerson>> matchingPersons =
                this.elasticsearchTemplate.search(searchQuery, PameasPerson.class, IndexCoordinates.of("pameas-person-" + date))
                        .stream().filter(result -> {
                            try {
                                return cryptoUtils.decryptBase64Message(result.getContent().getPersonalInfo().getPersonalId()).equals(personalIdentifier);
                            } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException |
                                     IllegalBlockSizeException | BadPaddingException e) {
                                log.error(e.getMessage());
                                return false;
                            }
                        }).collect(Collectors.toList());
        if (matchingPersons.size() > 0) {
            return Optional.of(matchingPersons.get(0).getContent());
        }
        return Optional.empty();
    }

    @Override
    public Optional<PameasPerson> getPersonBySurname(String surname) {
        Query searchQuery = new NativeSearchQueryBuilder()
                .withQuery(matchQuery("personalInfo.surname", surname).minimumShouldMatch("100%"))
                .build();
        SearchHits<PameasPerson> matchingPersons =
                this.elasticsearchTemplate.search(searchQuery, PameasPerson.class, IndexCoordinates.of("pameas-person"));
        if (matchingPersons.getTotalHits() > 0) {
            return Optional.of(matchingPersons.getSearchHit(0).getContent());
        }
        return Optional.empty();
    }

    @Override
    public Optional<PameasPerson> getPersonByHashedMacAddress(String hashedMacAddress) {
        String date = DateTimeFormatter.ofPattern("yyyy.MM.dd").format(LocalDate.now());
        Query searchQuery = new NativeSearchQueryBuilder()
                .withQuery(matchQuery("networkInfo.deviceInfoList.hashedMacAddress", hashedMacAddress).minimumShouldMatch("100%"))
                .build();
        SearchHits<PameasPerson> matchingPersons =
                this.elasticsearchTemplate.search(searchQuery, PameasPerson.class, IndexCoordinates.of("pameas-person-" + date));
        if (matchingPersons.getTotalHits() > 0) {
            return Optional.of(matchingPersons.getSearchHit(0).getContent());
        }
        return Optional.empty();
    }

    @Override
    public Optional<PameasPerson> getPersonByMacAddress(String macAddress) {
        String date = DateTimeFormatter.ofPattern("yyyy.MM.dd").format(LocalDate.now());
        Query searchQuery = new NativeSearchQueryBuilder()
                .withQuery(matchQuery("networkInfo.deviceInfoList.macAddress", macAddress).minimumShouldMatch("100%"))
                .build();
        SearchHits<PameasPerson> matchingPersons =
                this.elasticsearchTemplate.search(searchQuery, PameasPerson.class, IndexCoordinates.of("pameas-person-" + date));
        if (matchingPersons.getTotalHits() > 0) {
            return Optional.of(matchingPersons.getSearchHit(0).getContent());
        }
        return Optional.empty();
    }

    @Override
    public Optional<PameasPerson> getPersonByTicketNumber(String ticketNumber) {
        String date = DateTimeFormatter.ofPattern("yyyy.MM.dd").format(LocalDate.now());
        Query searchQuery = new NativeSearchQueryBuilder()
                .withQuery(matchQuery("personalInfo.ticketNumber", ticketNumber).minimumShouldMatch("100%"))
                .build();
        SearchHits<PameasPerson> matchingPersons =
                this.elasticsearchTemplate.search(searchQuery, PameasPerson.class, IndexCoordinates.of("pameas-person-" + date));
        if (matchingPersons.getTotalHits() > 0) {
            return Optional.of(matchingPersons.getSearchHit(0).getContent());
        }
        return Optional.empty();
    }


    @Override
    public Optional<PameasPerson> getPersonByMumbleName(String mumbleName) {
        String date = DateTimeFormatter.ofPattern("yyyy.MM.dd").format(LocalDate.now());
        Query searchQuery = new NativeSearchQueryBuilder()
                .withQuery(matchQuery("networkInfo.messagingAppClientId", mumbleName).minimumShouldMatch("100%"))
                .build();
        SearchHits<PameasPerson> matchingPersons =
                this.elasticsearchTemplate.search(searchQuery, PameasPerson.class, IndexCoordinates.of("pameas-person-" + date));
        if (matchingPersons.getTotalHits() > 0) {
            return Optional.of(matchingPersons.getSearchHit(0).getContent());
        }
        return Optional.empty();
    }


    @Override
    public Optional<PameasPerson> getPersonByBraceletId(String braceletId) {
        String date = DateTimeFormatter.ofPattern("yyyy.MM.dd").format(LocalDate.now());
        Query searchQuery = new NativeSearchQueryBuilder()
                .withQuery(matchQuery("networkInfo.braceletId", braceletId).minimumShouldMatch("100%"))
                .build();
        SearchHits<PameasPerson> matchingPersons =
                this.elasticsearchTemplate.search(searchQuery, PameasPerson.class, IndexCoordinates.of("pameas-person-" + date));
        if (matchingPersons.getTotalHits() > 0) {
            return Optional.of(matchingPersons.getSearchHit(0).getContent());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Geofence> getGeofenceByName(String name) {
        String date = DateTimeFormatter.ofPattern("yyyy.MM.dd").format(LocalDate.now());
        Query searchQuery = new NativeSearchQueryBuilder()
                .withQuery(matchQuery("gfName", name).minimumShouldMatch("100%"))
                .build();
        SearchHits<Geofence> matchingGeofence =
                this.elasticsearchTemplate.search(searchQuery, Geofence.class, IndexCoordinates.of("pameas-geofence-" + date));
        if (matchingGeofence.getTotalHits() > 0) {
            return Optional.of(matchingGeofence.getSearchHit(0).getContent());
        }
        return Optional.empty();
    }

    @Override
    public List<Geofence> getGeofences() {
        String date = DateTimeFormatter.ofPattern("yyyy.MM.dd").format(LocalDate.now());
        Query searchQuery = new NativeSearchQueryBuilder()
                .build();
        SearchHits<Geofence> matchingGeofence =
                this.elasticsearchTemplate.search(searchQuery, Geofence.class, IndexCoordinates.of("pameas-geofence-" + date));
        if (matchingGeofence.getTotalHits() > 0) {
            return matchingGeofence.stream().map(SearchHit::getContent).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }



    // personalIdentifier in plain text,
    @Override
    public List<PameasPerson> getAllPersonsDecrypted() {
        String date = DateTimeFormatter.ofPattern("yyyy.MM.dd").format(LocalDate.now());

        Query searchQuery = new NativeSearchQueryBuilder()
//                .withQuery(matchQuery("personalInfo.personalId",personalIdentifier).minimumShouldMatch("100%"))
                .build();
        return
                this.elasticsearchTemplate.search(searchQuery, PameasPerson.class, IndexCoordinates.of("pameas-person-" + date))
                        .stream().map(SearchHit::getContent).map(pameasPerson -> {
                            try {
                                pameasPerson.getPersonalInfo().setName(cryptoUtils.decryptBase64Message(pameasPerson.getPersonalInfo().getName()));
                                pameasPerson.getPersonalInfo().setSurname(cryptoUtils.decryptBase64Message(pameasPerson.getPersonalInfo().getSurname()));
                                pameasPerson.getPersonalInfo().setPersonalId(cryptoUtils.decryptBase64Message(pameasPerson.getPersonalInfo().getPersonalId()));
                                pameasPerson.getPersonalInfo().getTicketInfo().forEach(ticketInfo -> {
                                    try {
                                        ticketInfo.setSurname(cryptoUtils.decryptBase64Message(ticketInfo.getSurname()));
                                        ticketInfo.setName(cryptoUtils.decryptBase64Message(ticketInfo.getName()));
                                    } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException |
                                             IllegalBlockSizeException | BadPaddingException e) {
                                        log.error(e.getMessage());
                                    }
                                });

                            } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException |
                                     IllegalBlockSizeException | BadPaddingException e) {
                                log.error(e.getMessage());
                            }
                            return pameasPerson;
                        }).collect(Collectors.toList());


    }


    @Override
    public List<PameasPerson> getAllPassengersDecrypted() {
        String date = DateTimeFormatter.ofPattern("yyyy.MM.dd").format(LocalDate.now());

        Query searchQuery = new NativeSearchQueryBuilder()
                .withQuery(matchQuery("personalInfo.role", "passenger").minimumShouldMatch("100%"))
                .build();
        return
                this.elasticsearchTemplate.search(searchQuery, PameasPerson.class, IndexCoordinates.of("pameas-person-" + date))
                        .stream().map(SearchHit::getContent).map(pameasPerson -> {
                            try {
                                pameasPerson.getPersonalInfo().setName(cryptoUtils.decryptBase64Message(pameasPerson.getPersonalInfo().getName()));
                                pameasPerson.getPersonalInfo().setSurname(cryptoUtils.decryptBase64Message(pameasPerson.getPersonalInfo().getSurname()));
                                pameasPerson.getPersonalInfo().setPersonalId(cryptoUtils.decryptBase64Message(pameasPerson.getPersonalInfo().getPersonalId()));
                                pameasPerson.getPersonalInfo().getTicketInfo().forEach(ticketInfo -> {
                                    try {
                                        ticketInfo.setSurname(cryptoUtils.decryptBase64Message(ticketInfo.getSurname()));
                                        ticketInfo.setName(cryptoUtils.decryptBase64Message(ticketInfo.getName()));
                                    } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException |
                                             IllegalBlockSizeException | BadPaddingException e) {
                                        log.error(e.getMessage());
                                    }
                                });

                            } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException |
                                     IllegalBlockSizeException | BadPaddingException e) {
                                log.error(e.getMessage());
                            }
                            return pameasPerson;
                        }).collect(Collectors.toList());


    }


    @Override
    public List<PameasPerson> getAllCrewMembersDecrypted() {
        String date = DateTimeFormatter.ofPattern("yyyy.MM.dd").format(LocalDate.now());

        Query searchQuery = new NativeSearchQueryBuilder()
                .withQuery(matchQuery("personalInfo.role", "crew").minimumShouldMatch("100%"))
                .build();
        return
                this.elasticsearchTemplate.search(searchQuery, PameasPerson.class, IndexCoordinates.of("pameas-person-" + date))
                        .stream().map(SearchHit::getContent).map(pameasPerson -> {
                            try {
                                pameasPerson.getPersonalInfo().setName(cryptoUtils.decryptBase64Message(pameasPerson.getPersonalInfo().getName()));
                                pameasPerson.getPersonalInfo().setSurname(cryptoUtils.decryptBase64Message(pameasPerson.getPersonalInfo().getSurname()));
                                pameasPerson.getPersonalInfo().setPersonalId(cryptoUtils.decryptBase64Message(pameasPerson.getPersonalInfo().getPersonalId()));
                                pameasPerson.getPersonalInfo().getTicketInfo().forEach(ticketInfo -> {
                                    try {
                                        ticketInfo.setSurname(cryptoUtils.decryptBase64Message(ticketInfo.getSurname()));
                                        ticketInfo.setName(cryptoUtils.decryptBase64Message(ticketInfo.getName()));
                                    } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException |
                                             IllegalBlockSizeException | BadPaddingException e) {
                                        log.error(e.getMessage());
                                    }
                                });

                            } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException |
                                     IllegalBlockSizeException | BadPaddingException e) {
                                log.error(e.getMessage());
                            }
                            return pameasPerson;
                        }).collect(Collectors.toList());


    }


    @Override
    public void updatePerson(String personIdentifierDecrypted, PameasPerson person) {
        Optional<PameasPerson> matchingPerson = this.getPersonByPersonalIdentifierDecrypted(personIdentifierDecrypted);
        if (matchingPerson.isPresent()) {
            PameasPerson fetchedPerson = matchingPerson.get();
            try {
                PameasPersonUtils.updatePerson(fetchedPerson, person, cryptoUtils);
                this.personRepository.save(fetchedPerson);
            } catch (Exception e) {
                log.error(e.getMessage());
            }

        }
    }

    @Override
    public void save(PameasPerson person) {
        this.personRepository.save(person);
    }

    @Override
    public Optional<EvacuationStatus> getEvacuationStatus() {
        return evacuationStatusRepository.findStatus().stream().findFirst();
    }

    @Override
    public void saveEvacuationStatus(EvacuationStatus evacuationStatus) {
        try {
            Optional<EvacuationStatus> existingStatus = evacuationStatusRepository.findStatus().stream().findFirst();
            if (existingStatus.isPresent()) {
                existingStatus.get().setStatus(evacuationStatus.getStatus());
                evacuationStatusRepository.save(existingStatus.get());
            } else {
                evacuationStatusRepository.save(evacuationStatus);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            evacuationStatusRepository.save(evacuationStatus);
        }

    }



}



