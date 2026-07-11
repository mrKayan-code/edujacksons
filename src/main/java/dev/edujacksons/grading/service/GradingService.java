package dev.edujacksons.grading.service;

import dev.edujacksons.common.api.ForbiddenException;
import dev.edujacksons.grading.api.dto.*;
import dev.edujacksons.grading.domain.*;
import dev.edujacksons.grading.repository.ReviewRepository;
import dev.edujacksons.problems.service.ProblemDirectory;
import dev.edujacksons.submissions.domain.SubmissionStatus;
import dev.edujacksons.submissions.service.SubmissionDirectory;
import dev.edujacksons.submissions.service.SubmissionView;
import dev.edujacksons.judge.api.Verdict;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GradingService {

    private final ReviewRepository reviewRepository;
    private final SubmissionDirectory submissionDirectory;
    private final ProblemDirectory problemDirectory;

    public ReviewResponse createReview(UUID submissionId, CreateReviewRequest request, UUID actorId) {
        SubmissionView submission = submissionDirectory.find(submissionId)
                .orElseThrow(SubmissionNotFoundException::new);

        if (reviewRepository.findBySubmissionId(submissionId).isPresent()) {
            throw new ReviewAlreadyExistsException();
        }

        if (!problemDirectory.ownsProblem(actorId, submission.problemId())) {
            throw new ForbiddenException("Только владелец задачи может выставить оценку");
        }

        if (request.publish() && !isPublishable(request.score(), request.feedback())) {
            throw new ReviewNotPublishableException();
        }

        Review review = new Review(
                submissionId,
                submission.problemId(),
                submission.studentId(),
                actorId,
                request.publish() ? ReviewStatus.PUBLISHED : ReviewStatus.DRAFT,
                request.score(),
                request.feedback(),
                request.publish() ? OffsetDateTime.now() : null
        );

        Review saved = reviewRepository.save(review);
        return mapToResponse(saved);
    }

    public ReviewResponse updateReview(UUID reviewId, UpdateReviewRequest request, UUID actorId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(ReviewNotFoundException::new);

        if (!problemDirectory.ownsProblem(actorId, review.getProblemId())) {
            throw new ForbiddenException("Только владелец задачи может править оценку");
        }

        Integer finalScore = request.score() != null ? request.score() : review.getScore();
        String finalFeedback = request.feedback() != null ? request.feedback() : review.getFeedback();
        
        if (Boolean.TRUE.equals(request.publish())) {
            if (!isPublishable(finalScore, finalFeedback)) {
                throw new ReviewNotPublishableException();
            }
            if (review.getStatus() == ReviewStatus.DRAFT) {
                review.setStatus(ReviewStatus.PUBLISHED);
                review.setPublishedAt(OffsetDateTime.now());
            }
        }

        if (request.score() != null) review.setScore(request.score());
        if (request.feedback() != null) review.setFeedback(request.feedback());
        review.setReviewerId(actorId);

        return mapToResponse(reviewRepository.save(review));
    }

    public ReviewResponse getReviewForTeacher(UUID reviewId) {
        return reviewRepository.findById(reviewId)
                .map(this::mapToResponse)
                .orElseThrow(ReviewNotFoundException::new);
    }

    public ReviewResponse getReviewForTeacherBySubmission(UUID submissionId) {
        return reviewRepository.findBySubmissionId(submissionId)
                .map(this::mapToResponse)
                .orElseThrow(ReviewNotFoundException::new);
    }

    public StudentReviewResponse getReviewForStudent(UUID reviewId, UUID actorId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(ReviewNotFoundException::new);

        if (review.getStatus() == ReviewStatus.DRAFT || !review.getStudentId().equals(actorId)) {
            throw new ReviewNotFoundException();
        }

        return new StudentReviewResponse(
                review.getSubmissionId(),
                review.getScore(),
                review.getFeedback(),
                review.getPublishedAt()
        );
    }

    public StudentReviewResponse getReviewForStudentBySubmission(UUID submissionId, UUID actorId) {
        Review review = reviewRepository.findBySubmissionId(submissionId)
                .orElseThrow(ReviewNotFoundException::new);

        if (review.getStatus() == ReviewStatus.DRAFT || !review.getStudentId().equals(actorId)) {
            throw new ReviewNotFoundException();
        }

        return new StudentReviewResponse(
                review.getSubmissionId(),
                review.getScore(),
                review.getFeedback(),
                review.getPublishedAt()
        );
    }

    public List<GradebookEntryResponse> getGradebook(UUID problemId, UUID actorId, Boolean reviewed) {
        if (!problemDirectory.ownsProblem(actorId, problemId)) {
            throw new ForbiddenException("Доступ к журналу только для владельца задачи");
        }

        List<SubmissionView> submissions = submissionDirectory.listByProblem(problemId);
        Map<UUID, Review> reviewsMap = reviewRepository.findByProblemId(problemId).stream()
                .collect(Collectors.toMap(Review::getSubmissionId, r -> r));

        List<GradebookEntryResponse> entries = submissions.stream()
                .map(s -> {
                    Review review = reviewsMap.get(s.id());
                    String reviewStatus = "NOT_REVIEWED";
                    if (review != null) {
                        reviewStatus = review.getStatus().name();
                    }

                    return new GradebookEntryResponse(
                            s.id(),
                            s.studentId(),
                            s.status(),
                            s.verdict(),
                            s.submittedAt().atOffset(java.time.ZoneOffset.UTC),
                            reviewStatus,
                            review != null ? review.getId() : null,
                            review != null ? review.getScore() : null
                    );
                })
                .filter(e -> {
                    if (reviewed == null) return true;
                    boolean isPublished = "PUBLISHED".equals(e.reviewStatus());
                    return reviewed ? isPublished : !isPublished;
                })
                .collect(Collectors.toList());

        return entries;
    }

    public List<StudentReviewResponse> getMyReviews(UUID problemId, UUID actorId) {
        List<SubmissionView> submissions = submissionDirectory.listByProblem(problemId);
        
        return submissions.stream()
                .filter(s -> s.studentId().equals(actorId))
                .map(s -> reviewRepository.findBySubmissionId(s.id()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(r -> r.getStatus() == ReviewStatus.PUBLISHED)
                .map(r -> new StudentReviewResponse(
                        r.getSubmissionId(),
                        r.getScore(),
                        r.getFeedback(),
                        r.getPublishedAt()
                ))
                .collect(Collectors.toList());
    }

    private boolean isPublishable(Integer score, String feedback) {
        return score != null || (feedback != null && !feedback.isBlank());
    }

    private ReviewResponse mapToResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getSubmissionId(),
                review.getProblemId(),
                review.getStudentId(),
                review.getReviewerId(),
                review.getStatus(),
                review.getScore(),
                review.getFeedback(),
                review.getPublishedAt(),
                review.getCreatedAt().atOffset(java.time.ZoneOffset.UTC),
                review.getUpdatedAt().atOffset(java.time.ZoneOffset.UTC)
        );
    }
}
