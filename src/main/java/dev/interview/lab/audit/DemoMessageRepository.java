package dev.interview.lab.audit;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DemoMessageRepository extends MongoRepository<DemoMessageDocument, String> {
  List<DemoMessageDocument> findTop50ByChannelOrderByPublishedAtDesc(String channel);
}
