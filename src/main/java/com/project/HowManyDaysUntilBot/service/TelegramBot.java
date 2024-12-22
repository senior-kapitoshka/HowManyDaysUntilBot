package com.project.HowManyDaysUntilBot.service;

import com.project.HowManyDaysUntilBot.config.BotConfig;
import com.project.HowManyDaysUntilBot.model.*;
import com.project.HowManyDaysUntilBot.utils.*;
import lombok.extern.slf4j.Slf4j;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.project.HowManyDaysUntilBot.service.Call.*;
import static com.project.HowManyDaysUntilBot.utils.Sender.*;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    @Autowired
    private HolidayRepository holidayRepository;

    private Searcher searcher ;
    private Utils utils;
    Date todaysDate;
    private FileManager fileManager;
    Call call = START;
    Holiday holiday=null;

    Long chatId;


    // TelegramLongPollingBot сам проверяет о наличии входящих уведомлений
    final private BotConfig config;
    final static private String ABOUT= """
            Бот дает возможность получить информацию 
            о праздниках, которые отмечают на территории
            СНГ и \uD83C\uDDF7\uD83C\uDDFA РФ \uD83C\uDDF7\uD83C\uDDFA .
            Для этого нужно выбрать и нажать соответствующую  
            кнопку на панели и в зависимости от запроса
            ввести название месяца/праздника.
            Ввод осуществляется на кириллице, если забыли
            полное название праздника, бот предложит подходящее.
            
            /start начать заново
            
            вопросы/предложения @senior_kapitoshka
            """;
    final static private String TO_ADD= """
            Для добавления праздника в базу нужно ввести данные в формате:
            [день - целое число] [месяц - целое число] [название праздника]
            """;

    public TelegramBot(BotConfig c){
        todaysDate = new Date();
        utils = new Utils(todaysDate);
        searcher = new Searcher();
        fileManager= new FileManager();
        config=c;
        List<BotCommand> listOfCommands=new ArrayList();
        listOfCommands.add(new BotCommand("/start","start or reboot"));
        listOfCommands.add(new BotCommand("/add","add item"));
        listOfCommands.add(new BotCommand("/json","get all holidays in one .json file"));
        listOfCommands.add(new BotCommand("/about","get help and credits"));
        try{
            execute(new SetMyCommands(listOfCommands,new BotCommandScopeDefault(),null));
        }catch(TelegramApiException e){
            log.error("TelegramBotError");
        }
    }
    @Override
    public void onUpdateReceived(Update update) {

        if(update.hasMessage() && update.getMessage().hasText()){
                String textMsg= update.getMessage().getText();
                chatId=update.getMessage().getChatId();
                Object res=null;
                switch(textMsg){
                    case "/start":
                        telegramExec(buttonRowsSetting(chatId));
                        log.info("app start");
                        break;
                    case "/add":
                        telegramExec(sendMessage(chatId,TO_ADD));
                        call = ADD;
                        break;
                    case "/about":
                        telegramExec(sendMessage(chatId,ABOUT));
                        log.info("send information about app");
                        break;
                    case "/json":
                        telegramExec(sendDocument(chatId,getJson()));
                        telegramExec(buttonRowsSetting(chatId));
                        log.info("get json");
                        break;
                    case "/y":
                        holidayRepository.save(holiday);
                        telegramExec(sendMessage(chatId,"Успешно добавлено"));
                        telegramExec(buttonRowsSetting(chatId));
                        break;
                    case "/n":
                        holiday=null;
                        telegramExec(sendMessage(chatId,"Обнуляем ввод"));
                        telegramExec(buttonRowsSetting(chatId));
                        break;

                    default:
                        if(call == MONTH){
                            res = showDates(textMsg);
                        }else if(call==DIFF) {
                            var m= Pattern.compile("([\\а-яёА-ЯЁ ]+)").matcher(textMsg);
                            while(m.find()) {
                                res = returnDiff(m.group(1).trim().toLowerCase());
                            }


                        }else if(call==ADD){
                            res = addItemToBase(textMsg);

                        }


                        if((call==MONTH || call== DIFF) && res == null){
                            telegramExec(sendMessage(chatId, "попробуйте ввести заново"));
                        }else if((call==MONTH || call== DIFF || call==ADD) && res != null){
                            if(res instanceof String){
                                telegramExec(sendMessage(chatId, (String)res));
                            }
                            else if(res instanceof SendPhoto){
                                telegramExec((SendPhoto)res);
                            }
                            call=START;
                        }else if(call==WAIT){
                            telegramExec(sendMessage(chatId, (String)res));
                            telegramExec(sendMessage(chatId,"/y для добавления, /n отмена"));
                        }
                        if(call==START) telegramExec(buttonRowsSetting(chatId));
                }
        }else if(update.hasCallbackQuery()){
            String textMsg= update.getCallbackQuery().getData();
            chatId=update.getCallbackQuery().getMessage().getChatId();
            Object res=null;
            switch(textMsg){
                case "holiday":
                    res = todaysHoliday();
                    call=HOLIDAY;
                    break;
                case "month": res = "Введите название месяца";
                    call=MONTH;
                    break;
                case "howManyDaysUntil": res = "Введите название праздника";
                    call=DIFF;
                    break;
                case "state":
                    res = stateHoliday();
                    call=STATE;
                    break;
            }
            if(res instanceof String){
                telegramExec(sendMessage(chatId, (String)res));
            }else if(res instanceof SendPhoto){
                telegramExec((SendPhoto)res);
            }else if(res instanceof SendMessage){
                telegramExec((SendMessage)res);
            }
            if(call==HOLIDAY || call==STATE)telegramExec(buttonRowsSetting(chatId));
        }
    }

    private String showDates(String txt){
        List<Holiday> holidays = null;
        int month = searcher.haveMonth(txt.toLowerCase().trim());
        if(month != -1){
            holidays = holidayRepository.findByMonth(month+1);
        }else{
            String trying = searcher.findMostSimilarMonth(txt.toLowerCase().trim());
            if(trying!=null){
                month = searcher.haveMonth(trying.toLowerCase().trim());
                return "Возможно вы имели в виду "+ trying
                        +"?\n\n"+ utils.holidaysToString(holidayRepository.findByMonth(month+1));
           }
        }
        return holidays==null?
                "Неверный ввод":
                utils.holidaysToString(holidays);
    }


    private Object returnDiff(String holidayTxt){
        Optional<Holiday> result = holidayRepository.findByHoliday(holidayTxt.toLowerCase().trim());
        if(result.isPresent()){
            Object isExclusive = returnForExclusiveHoliday(result.get());
            if(isExclusive!=null) return isExclusive;
            Pair<String,Long> pair = utils.calculateDiff(utils.holidayToDate(result.get()));
            return pair.first;
        }

        searcher.setDictionary(holidayRepository.getAllHolidays());
        String wordToSearch = searcher.peekTag(holidayTxt);
        if(wordToSearch==null) return "Ничего не найдено";
        List<Holiday> listOfPossibleHolidays = holidayRepository.findHolidayByWord(wordToSearch);
        if(listOfPossibleHolidays!=null){
            Pair<String,Long> pair = utils.calculateDiff(utils.holidayToDate(listOfPossibleHolidays.get(0)));
            return "Возможно вы имели в виду " + listOfPossibleHolidays.get(0).getHolidayName() + " ?\n" +pair.first;
        }
        return "Ничего не найдено";
    }

     private Object returnForExclusiveHoliday(Holiday holiday){
        Object result=null;
        if(holiday.getHolidayName().toLowerCase().trim().equals("новый год") || holiday.getHolidayName().toLowerCase().trim().equals("день победы")){
            Pair<String,Long> str_diff= utils.calculateDiff(utils.holidayToDate(holiday));
            if(str_diff.second <= 30 && holiday.getHolidayName().toLowerCase().trim().equals("новый год")){
                result = sendPhoto(chatId, fileManager.choosingPhotoToScan("ng_mchitsya"), str_diff.first);
            }else if(holiday.getHolidayName().toLowerCase().trim().equals("день победы")){
                result = sendPhoto(chatId, fileManager.choosingPhotoToScan(
                                (new Random().nextInt(6))%2==1?"vd1":"vd2"),
                        "До Священного Праздника День Победы осталось " + str_diff.second+ utils.getSufx(str_diff.second));
            }
        }
        return result;
    }

    private String getJson(){
        var holidays = holidayRepository.getAllHolidayObjects();
        JSONArray jsonArray = new JSONArray();
        for(Holiday h:holidays){
            JSONObject jsonObject = new JSONObject();
            jsonObject .put("day_month", h.getDayMonth());
            jsonObject .put("holiday_name", h.getHolidayName());
            jsonObject .put("month", h.getMonth());
            jsonObject .put("day", h.getDay());
            jsonObject .put("is_state", h.isState());
            jsonArray.add(jsonObject );
        }
        return fileManager.JSONWriter(jsonArray);
    }

    private Object todaysHoliday(){
        Optional<Holiday> holiday = holidayRepository.findById(utils.dateToPrimaryKey(todaysDate));
        if(holiday.isPresent()){
            return sendPhoto(chatId, fileManager.choosingPhotoToScan(utils.dateToPrimaryKey(todaysDate)), holiday.get().getHolidayName());
        }
        Calendar c = Calendar.getInstance();
        c.setTime(todaysDate);
        return c.get(Calendar.DAY_OF_WEEK) == 7 ?
                sendPhoto(chatId, fileManager.choosingPhotoToScan("saturday"), "Сегодня ничего не празднуют, но можно просто расслабиться и отдохнуть, суббота!") :
                c.get(Calendar.DAY_OF_WEEK) == 1 ?
                        sendPhoto(chatId, fileManager.choosingPhotoToScan("sunday"), "Сегодня ничего не празднуют, но можно просто расслабиться и отдохнуть, воскресенье!") :
                        sendMessage(chatId, "Сегодня ничего не празднуют");
    }

    private Object stateHoliday(){
        List<Holiday> holidays = holidayRepository.findByState();
        return sendPhoto(chatId, fileManager.choosingPhotoToScan("russia"), utils.holidaysToString(holidays));
    }



    private void telegramExec(Object message) {
        try{
            if(message instanceof SendMessage){
                log.info("send message");
                execute((SendMessage)message);
            }else if(message instanceof SendPhoto){
                log.info("send photo");
                execute((SendPhoto)message);
            }else if(message instanceof SendDocument){
                log.info("send document");
                execute((SendDocument)message);
            }
        }catch(TelegramApiException e){
            log.error("TelegramBotError");
        }

    }

    public String addItemToBase(String txt){
        holiday = new Holiday();
        if(txt.matches("([\\d]+) ([\\d]+) ([\\а-яёА-ЯЁ ]+)")){
            Pattern p=Pattern.compile("([\\d]+) ([\\d]+) ([\\а-яёА-ЯЁ ]+)");
            Matcher m=p.matcher(txt);
                while(m.find()){
                    Integer dayMonth=utils.getDayMonth(Integer.parseInt(m.group(1)),Integer.parseInt(m.group(2)));
                    Integer day=Integer.parseInt(m.group(1));
                    Integer month=Integer.parseInt(m.group(2));
                    String holidayName=m.group(3);
                    boolean isState=false;

                    holiday.setHolidayName(holidayName);
                    holiday.setDay(day);
                    holiday.setDayMonth(dayMonth);
                    holiday.setMonth(month);
                    holiday.setState(isState);
                }
        }else{
            return "Ошибка добавления";
        }
        if(!holidayRepository.findById(holiday.getDayMonth()).isPresent()){
            holidayRepository.save(holiday);
            return "Успешно добавлено";
        }else{
            call=WAIT;
            return "В этот день уже есть праздник, сделать новую запись?";
        }

    }



    private SendMessage buttonRowsSetting(long chatId){

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        List<InlineKeyboardButton> row4 = new ArrayList<>();

        InlineKeyboardButton b1 = new InlineKeyboardButton();
        b1.setText("Какой сегодня праздник?");
        b1.setCallbackData("holiday");

        InlineKeyboardButton b2 = new InlineKeyboardButton();
        b2.setText("Сколько дней до праздника ...?");
        b2.setCallbackData("howManyDaysUntil");

        InlineKeyboardButton b3 = new InlineKeyboardButton();
        b3.setText("Какие праздники отмечают в ...<месяц>...?");
        b3.setCallbackData("month");
        InlineKeyboardButton b4 = new InlineKeyboardButton();
        b4.setText("\uD83C\uDDF7\uD83C\uDDFA Государственные праздники РФ \uD83C\uDDF7\uD83C\uDDFA");
        b4.setCallbackData("state");

        row1.add(b1);
        row2.add(b2);
        row3.add(b3);
        row4.add(b4);


        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(row4);


        markup.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выберите одну из возможностей: ");
        message.setReplyMarkup(markup);
        return message;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

}

