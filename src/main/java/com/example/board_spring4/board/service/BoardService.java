package com.example.board_spring4.board.service;

import com.example.board_spring4.board.dto.BoardRequestDto;
import com.example.board_spring4.board.dto.BoardResponseDto;
import com.example.board_spring4.board.entity.Board;
import com.example.board_spring4.board.repository.BoardRepository;
import com.example.board_spring4.comment.dto.CommentResponseDto;
import com.example.board_spring4.comment.entity.Comment;
import com.example.board_spring4.global.dto.StatusResponseDto;
import com.example.board_spring4.global.exception.ErrorException;
import com.example.board_spring4.global.exception.ErrorResponseDto;
import com.example.board_spring4.global.exception.ExceptionEnum;
import com.example.board_spring4.global.jwt.JwtUtil;
import com.example.board_spring4.user.entity.UserRoleEnum;
import com.example.board_spring4.user.entity.Users;
import com.example.board_spring4.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service //
@RequiredArgsConstructor // generates a constructor for the class that initializes all final fields
public class BoardService {

    //주입
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public ResponseEntity<?> createBoard(BoardRequestDto boardRequestDto, HttpServletRequest httpServletRequest) {
        try {
            String token = jwtUtil.resolveToken(httpServletRequest);
            Users users = getUserByToken(token);
            if (users != null) {
                Board board = new Board(boardRequestDto, users);
                boardRepository.save(board);
                BoardResponseDto boardResponseDto = new BoardResponseDto(board);
                return ResponseEntity.ok(boardResponseDto);
            } else {
                ErrorResponseDto errorResponseDto = new ErrorResponseDto(ExceptionEnum.TOKEN_NOT_FOUND);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponseDto);
            }
        } catch (ErrorException e) {
            ErrorResponseDto errorResponseDto = new ErrorResponseDto(e.getExceptionEnum().getMessage(), e.getExceptionEnum().getStatus());
            return ResponseEntity.status(errorResponseDto.getStatus()).body(errorResponseDto);
        }
    }

    public ResponseEntity<?> getBoard(Long id) {
        try {
            Board board = boardRepository.findById(id).orElseThrow(
                    () -> new ErrorException(ExceptionEnum.BOARD_NOT_FOUND)
            );

            List<CommentResponseDto> comments = new ArrayList<>();
            for (Comment comment : board.getComment()) {
                comments.add(new CommentResponseDto(comment));
            }

            BoardResponseDto boardResponseDto = new BoardResponseDto(board, comments);
            return ResponseEntity.ok(boardResponseDto);
        } catch (ErrorException e) {
            ErrorResponseDto errorResponseDto = new ErrorResponseDto(e.getExceptionEnum().getMessage(), e.getExceptionEnum().getStatus());
            return ResponseEntity.status(errorResponseDto.getStatus()).body(errorResponseDto);
        }
    }


    @Transactional(readOnly = true)
    public List<BoardResponseDto> getBoardList() {
        List<Board> boardList = boardRepository.findAllByOrderByModifiedAtDesc();

        List<BoardResponseDto> boards = new ArrayList<>();

        for (Board board : boardList){
            List<CommentResponseDto> comments = new ArrayList<>();
            for (Comment comment : board.getComment()) {
                comments.add(new CommentResponseDto(comment));
            }
            boards.add(new BoardResponseDto(board, comments));
        }
        return boards;
    }
    @Transactional
    public ResponseEntity<?> updateBoard(Long id, BoardRequestDto boardRequestDto, HttpServletRequest httpServletRequest) {
        String token = jwtUtil.resolveToken(httpServletRequest);

        try {
            Board board = boardRepository.findById(id).orElseThrow(
                    () -> new ErrorException(ExceptionEnum.BOARD_NOT_FOUND)
            );
            Users users = getUserByToken(token);
            assert users != null;
            if (board.getUsers().getUsername().equals(users.getUsername()) || users.getRole() == UserRoleEnum.ADMIN) {
                board.update(boardRequestDto);
                return ResponseEntity.ok(new BoardResponseDto(board));
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDto(ExceptionEnum.NOT_ALLOWED_AUTHORIZATIONS));
            }
        } catch (ErrorException e) {
            return ResponseEntity.status(e.getExceptionEnum().getStatus()).body(new ErrorResponseDto(e.getExceptionEnum()));
        }
    }


    public ResponseEntity<?> deleteBoard(Long id, HttpServletRequest httpServletRequest) {
        try {
            String token = jwtUtil.resolveToken(httpServletRequest);
            Claims claims = checkToken(httpServletRequest);
            Users users = userRepository.findByUsername(claims.getSubject()).orElseThrow(
                    () -> new ErrorException(ExceptionEnum.USER_NOT_FOUND)
            );
            Board board = boardRepository.findById(id).orElseThrow(
                    () -> new ErrorException(ExceptionEnum.BOARD_NOT_FOUND)
            );
            if (users.getUsername().equals(board.getUsers().getUsername()) || users.getRole() == UserRoleEnum.ADMIN) {
                boardRepository.deleteById(board.getId());
                return new ResponseEntity<>(new StatusResponseDto("게시글을 삭제하였습니다.", HttpStatus.OK.value()), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new ErrorResponseDto(ExceptionEnum.NOT_ALLOWED_AUTHORIZATIONS), HttpStatus.FORBIDDEN);
            }
        } catch (ErrorException e) {
            return new ResponseEntity<>(new ErrorResponseDto(e.getExceptionEnum()), HttpStatus.NOT_FOUND);
        }
    }


    private Users getUserByToken(String token) {
        try {
            Claims claims;

            if (token != null) {
                if (jwtUtil.validateToken(token)) {
                    claims = jwtUtil.getUserInfoFromToken(token);
                } else {
                    throw new ErrorException(ExceptionEnum.TOKEN_NOT_FOUND);
                }

                return userRepository.findByUsername(claims.getSubject()).orElseThrow(
                        ()-> new ErrorException(ExceptionEnum.USER_NOT_FOUND)
                );
            }
            return null;
        } catch (ErrorException e) {
            throw new ErrorException(e.getExceptionEnum());
        }
    }


    private Claims checkToken(HttpServletRequest httpServletRequest) {
        try {
            Claims claims = jwtUtil.getUserInfoFromToken(jwtUtil.resolveToken(httpServletRequest));
            if (claims == null) {
                throw new ErrorException(ExceptionEnum.TOKEN_NOT_FOUND);
            }
            return claims;
        } catch (ErrorException e) {
            throw new ErrorException(e.getExceptionEnum());
        }
    }
}
