package com.example.board_spring3.comment.service;

import com.example.board_spring3.board.entity.Board;
import com.example.board_spring3.board.repository.BoardRepository;
import com.example.board_spring3.comment.dto.CommentRequestDto;
import com.example.board_spring3.comment.dto.CommentResponseDto;
import com.example.board_spring3.global.dto.StatusResponseDto;
import com.example.board_spring3.comment.entity.Comment;
import com.example.board_spring3.comment.repository.CommentRepository;
import com.example.board_spring3.global.dto.InterfaceDto;
import com.example.board_spring3.global.jwt.JwtUtil;
import com.example.board_spring3.user.entity.UserRoleEnum;
import com.example.board_spring3.user.entity.Users;
import com.example.board_spring3.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final BoardRepository boardRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public InterfaceDto createComment(CommentRequestDto commentRequestDto, HttpServletRequest httpServletRequest) {

        String token = jwtUtil.resolveToken(httpServletRequest);

        Board board = boardRepository.findById(commentRequestDto.getBoard_id()).orElseThrow(
                ()-> new IllegalArgumentException("존재하지 않는 게시글입니다.")
        );

        Users users = getUserByToken(token);

        if(users != null){
        Comment comment = new Comment(commentRequestDto);

        comment.setBoard(board);
        comment.setUsers(users);

        commentRepository.save(comment);

        return new CommentResponseDto(comment);
        }else {
            return new StatusResponseDto("사용할 수 없는 토큰입니다.",HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional
    public InterfaceDto updateComment(Long id, CommentRequestDto commentRequestDto, HttpServletRequest httpServletRequest) {

        String token = jwtUtil.resolveToken(httpServletRequest);

        Users users = getUserByToken(token);

        Comment comment = commentRepository.findById(id).orElseThrow(
                ()-> new IllegalArgumentException("해당 댓글이 없습니다.")
        );

        if(comment.getUsers().getUsername().equals(users.getUsername()) || users.getRole() == UserRoleEnum.ADMIN){
            comment.updateComment(commentRequestDto);

            return new CommentResponseDto(comment);
        } else {
          return new StatusResponseDto("해당 댓글의 작성자가 아닙니다",HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional
    public StatusResponseDto deleteComment(Long id, HttpServletRequest httpServletRequest) {
        String token = jwtUtil.resolveToken(httpServletRequest);
        Users users = getUserByToken(token);

        Comment comment = commentRepository.findById(id).orElseThrow(
                ()-> new IllegalArgumentException("존재하지 않는 댓글입니다.")
        );
        if (comment.getUsers().getUsername().equals(users.getUsername()) || users.getRole() == UserRoleEnum.ADMIN){
            commentRepository.delete(comment);

            return new StatusResponseDto("해당 댓글을 삭제하였습니다.", HttpStatus.OK);
        } else {
            return new StatusResponseDto("해당 댓글을 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private Users getUserByToken(String token) {
        Claims claims;

        if(token != null){
            if(jwtUtil.validateToken(token)){
                claims = jwtUtil.getUserInfoFromToken(token);
            }else{
                throw new IllegalArgumentException("Token Error");
            }

            Users users = userRepository.findByUsername(claims.getSubject()).orElseThrow(
                    () -> new IllegalArgumentException("존재하지 않는 사용자입니다.")
            );
            return users;
        }
        return null;
    }
}