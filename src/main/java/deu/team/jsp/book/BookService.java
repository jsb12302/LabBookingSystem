package deu.team.jsp.book;

import deu.team.jsp.account.AccountRepository;
import deu.team.jsp.account.domain.Account;
import deu.team.jsp.admin.warning.Warning;
import deu.team.jsp.admin.warning.WarningRepository;
import deu.team.jsp.alert.AlertService;
import deu.team.jsp.announce.AnnounceRepository;
import deu.team.jsp.announce.domain.Announcement;
import deu.team.jsp.book.domain.ApproveStatus;
import deu.team.jsp.book.domain.Book;
import deu.team.jsp.notification.NotificationService;
import deu.team.jsp.schedule.Schedule;
import deu.team.jsp.schedule.ScheduleRepository;
import jdk.swing.interop.SwingInterOpUtils;
import org.apache.tomcat.jni.Local;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class BookService {

  @Autowired
  BookRepository bookRepository;

  @Autowired
  AccountRepository accountRepository;

  @Autowired
  AnnounceRepository announceRepository;

  @Autowired
  ScheduleRepository scheduleRepository;

  @Autowired
  AlertService alertService;

  @Autowired
  WarningRepository warningRepository;

  @Autowired
  NotificationService notificationService;


  public void book(HttpServletRequest request, HttpServletResponse response) throws IOException {

    HttpSession httpSession = request.getSession();
    Account account = (Account) httpSession.getAttribute("account");

    String startTime = request.getParameter("startTime");
    String endTime = request.getParameter("endTime");
    String seat = request.getParameter("seat");
    String labNo = request.getParameter("labNo");


    int year = Integer.valueOf(startTime.substring(0, 4));
    int month = Integer.valueOf(startTime.substring(5, 7));

    int startDay=Integer.valueOf(startTime.substring(8, 10));
    int endDay = Integer.valueOf(endTime.substring(8, 10));

    int startHour = Integer.valueOf(startTime.substring(11, 13));
    int startMinute = Integer.valueOf(startTime.substring(14, 16));
    int endHour = Integer.valueOf(endTime.substring(11, 13));
    int endMinute = Integer.valueOf(endTime.substring(14, 16));

    if (Objects.isNull(seat)) {
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      out.println("<script>alert('????????? ?????? ????????? ????????? ?????? ????????? ????????? ?????? ?????? ?????????.'); location.href='/studentPage';</script>");
      out.flush();
    }

    int seatX = Integer.valueOf(seat.substring(0, 1));
    int seatY = Integer.valueOf(seat.substring(2, 3));

    LocalDateTime start = LocalDateTime.of(year, month, startDay, startHour, startMinute);
    LocalDateTime end = LocalDateTime.of(year, month, endDay, endHour, endMinute);

    // 4??? 30??? ????????? ??????
    LocalTime flagTime = LocalTime.of(16, 30);
    Book book;
    if(end.toLocalTime().isBefore(flagTime)) {
      book = new Book(account.getStudentId(), start, end, labNo, seatX, seatY, ApproveStatus.APPROVE);

      //?????? ????????? ??????????????? ??????
      try {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(account.getStudentId()).append("?????? ?????? ????????? ?????????????????? ???????????? ???????????????.");
        String content = stringBuilder.toString();
        notificationService.addNotification(account.getStudentId(), content);
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      }

    }else{
      book = new Book(account.getStudentId(), start, end, labNo, seatX, seatY, ApproveStatus.READY);
    }
    Book findSeat = bookRepository.findSeat(seatX, seatY, labNo, start, end);

    List<Announcement> all = announceRepository.findAll();
    String announceContent = "";
    if (all.size() == 1) { //???????????? ?????? ????????? ???
      Announcement announcement = all.get(0);
      announceContent = announcement.getAnnounceContent();
    } else {
      announceContent = "????????? ?????? ???????????????.";
    }

    //?????? ???????????? ???????????? ???????????? ???????????? ????????? ????????? ???????????? ????????? ???????????? 0?????? ??????
    List<Book> lastBookList = bookRepository.getLastBookList(account.getStudentId());
    LocalDateTime now = LocalDateTime.now();
    if (lastBookList.size() > 0) {
      Book lastBook = lastBookList.get(lastBookList.size() - 1);
      if (lastBook.getEndTime().isBefore(now)) {
        accountRepository.updateBookStatus(account.getStudentId(), 0);
      }
    }

    LocalDateTime limitBookTime=LocalDateTime.of(year, month, startDay, 16, 30);
//        if(now.isAfter(limitBookTime)){ //?????? ?????? 4??? ??? ?????? ?????? ??????
//            alertService.alertMessage("?????? 4??? ??? ?????? ????????? ?????? ?????????.","/studentPage",response);
//        }else{
    //?????? ?????? ?????? ??????
    int bookStatus = accountRepository.findByStudentId(account.getStudentId()).getBookStatus();
    if(Objects.nonNull(findSeat) || bookStatus==1) {
      alertService.alertMessage("?????? ????????? ?????? ????????? ?????? ????????? ????????? ?????????.", "/studentPage", response);
    }else{
      Account byStudentId = accountRepository.findByStudentId(account.getStudentId());
      if(byStudentId.getBookStatus()==2){
        alertService.alertMessage("?????? 3??? ?????? ????????? ????????? ????????? ?????????.", "/studentPage", response);
      }else{
        if (Objects.isNull(findSeat) && bookStatus == 0) { //???????????? ?????? ?????? ???
          bookRepository.save(book);
          accountRepository.updateBookStatus(account.getStudentId(), 1);
          alertService.alertMessage(announceContent, "", response);
        }
      }
    }

  }

  public int[][] checkSeat(HttpServletRequest request, HttpServletResponse response, Model model) throws IOException {

    String startTime = request.getParameter("startTime");
    String endTime = request.getParameter("endTime");
    String labNo = request.getParameter("labNo");

    int year = Integer.valueOf(startTime.substring(0, 4));
    int month = Integer.valueOf(startTime.substring(5, 7));

    int startDay=Integer.valueOf(startTime.substring(8, 10));
    int endDay=Integer.valueOf(endTime.substring(8, 10));

    int startHour = Integer.valueOf(startTime.substring(11, 13));
    int startMinute = Integer.valueOf(startTime.substring(14, 16));


    int endHour = Integer.valueOf(endTime.substring(11, 13));
    int endMinute = Integer.valueOf(endTime.substring(14, 16));

    //?????? ?????? ?????? ?????????
    LocalDate targetDate = LocalDate.of(year, month, startDay);
    DayOfWeek dayOfWeek = targetDate.getDayOfWeek();
    int targetDayOfWeek = dayOfWeek.getValue();
    String dayOfWeekKorean = "";
    if (targetDayOfWeek == 1) {
      dayOfWeekKorean = "???";
    } else if (targetDayOfWeek == 2) {
      dayOfWeekKorean = "???";
    } else if (targetDayOfWeek == 3) {
      dayOfWeekKorean = "???";
    } else if (targetDayOfWeek == 4) {
      dayOfWeekKorean = "???";
    } else if (targetDayOfWeek == 5) {
      dayOfWeekKorean = "???";
    } else if (targetDayOfWeek == 6) {
      dayOfWeekKorean = "???";
    } else if (targetDayOfWeek == 7) {
      dayOfWeekKorean = "???";
    }

    Integer dayOfWeekNumber = Integer.valueOf(request.getParameter("todayDayOfWeek"));
    if ((dayOfWeekNumber == 6 && targetDayOfWeek == 6) || (dayOfWeekNumber == 6 && targetDayOfWeek == 7)
            || (dayOfWeekNumber == 7 && targetDayOfWeek == 7)) {
      alertService.alertMessage("???????????? ?????? ?????? ?????? ????????? ?????????.","/studentPage",response);
    }

//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime limitBookTime=LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), 16, 30);
//
//        DayOfWeek dayOfWeek1 = now.getDayOfWeek();
//        int value = dayOfWeek1.getValue();
//        System.out.println(value);
//        System.out.println(targetDayOfWeek);
//        if((value==5) && (targetDayOfWeek ==6 || targetDayOfWeek==7) && (now.isAfter(limitBookTime))){
//            alertService.alertMessage("?????? 4??? ??? ?????? ????????? ?????? ?????????.","/studentPage",response);
//        }

    //????????? ?????? ?????? ?????? ?????????
    LocalDateTime start= LocalDateTime.of(year, month, startDay,startHour,startMinute);
    LocalDateTime end= LocalDateTime.of(year, month, endDay,endHour,endMinute);

    LocalTime bookStartTime=LocalTime.of(startHour, startMinute);
    LocalTime bookEndTime=LocalTime.of(endHour, endMinute);

    if(start.isAfter(end)){
      alertService.alertMessage("?????? ????????? ?????? ???????????? ????????? ????????????.","/bookPage",response);
    }

    if (Objects.isNull(labNo)) {
      labNo = "915";
      List<Book> books = bookRepository.bookList(labNo, start, end);
      if (books.size() >= 25) {
        labNo = "916";
        List<Book> books916 = bookRepository.bookList(labNo, start, end);
        if (books916.size() >= 25) {
          labNo = "918";
          List<Book> books918 = bookRepository.bookList(labNo, start, end);
          if (books918.size() >= 25) {
            labNo = "911";
          }
        }
      }
    }
    model.addAttribute("labNo", labNo);
    //???????????? ????????? ?????? ??????
    int labSizeX = 5;
    int labSizeY = 8;

    int[][] seats = new int[labSizeX][labSizeY];

    for (int i = 0; i < labSizeX; i++) { //???????????? ?????????
      for (int j = 0; j < labSizeY; j++) {
        seats[i][j] = 0;
      }
    }

    List<Schedule> schedules = scheduleRepository.scheduleList(dayOfWeekKorean, labNo, bookStartTime, bookEndTime);

    if (schedules.size() == 0) { //???????????? ???????????????

      List<Book> books = bookRepository.bookList(labNo, start, end);

      for (Book book : books) {
        for (int i = 0; i < labSizeX; i++) {
          for (int j = 0; j < labSizeY; j++) {
            if (book.getSeatX() - 1 == j && book.getSeatY() - 1 == i) {
              seats[i][j] = 1;
            }
          }
        }
      }

    } else {

      for (int i = 0; i < labSizeX; i++) {
        for (int j = 0; j < labSizeY; j++) {
          seats[i][j] = 1;
        }
      }
    }

    return seats;
  }

  public List<Book> getMyBookList(HttpServletRequest request) {
    HttpSession httpSession = request.getSession();
    Account account = (Account) httpSession.getAttribute("account");

    return bookRepository.MyBookList(account.getStudentId());
  }

  public void removeBook(Long bookId) {
    bookRepository.deleteById(bookId);
  }

  public void changeAccountBookStatus(HttpServletRequest request) {
    HttpSession httpSession = request.getSession();
    Account account = (Account) httpSession.getAttribute("account");

    accountRepository.updateBookStatus(account.getStudentId(), 0);
  }

}