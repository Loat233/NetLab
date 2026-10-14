package org.lostwind.netlab.service;

import org.lostwind.netlab.entity.Review;
import org.lostwind.netlab.mapper.ReviewMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {
    @Autowired
    ReviewMapper reviewMapper;
    @Autowired
    AccountService accountService;

    public List<Review> getReviews() {
        return reviewMapper.selectAll();
    }

    public void addReview(Integer userId, String content) {
        accountService.getAccountById(userId);

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("评论内容不能为空");
        }

        content = content.trim();

        if (content.length() > 500) {
            throw new IllegalArgumentException("评论内容不能超过500字");
        }

        Review review = new Review();
        review.setAccountId(userId);
        review.setContent(content);

        if (reviewMapper.insert(review) != 1) {
            throw new IllegalStateException("评论添加失败");
        }
    }

    public void deleteReview(Integer accountId, Integer reviewId) {
        accountService.getAccountById(accountId);

        if (reviewMapper.delete(reviewId, accountId) != 1) {
            throw new IllegalStateException("该评论已不存在");
        }
    }
}
