package com.kkwl.collector.utils;

import com.kkwl.collector.common.LogType;
import org.slf4j.Logger;
import org.slf4j.MDC;

public class LogTools {
  public static void log(Logger logger, String fileName, LogType type, String message) {
    MDC.put("LOG_FILE_NAME", fileName);
    switch (type) {
      case DEBUG:
        logger.debug(message);
        break;
      case INFO:
        logger.info(message);
        break;
      case WARN:
        logger.warn(message);
        break;
      case ERROR:
        logger.error(message);
        break;
    } 
    MDC.remove("LOG_FILE_NAME");
  }
  
  public static void log(Logger logger, String fileName, LogType type, String message, Throwable ex) {
    MDC.put("LOG_FILE_NAME", fileName);
    switch (type) {
      case DEBUG:
        logger.debug(message, ex);
        break;
      case INFO:
        logger.info(message, ex);
        break;
      case WARN:
        logger.warn(message, ex);
        break;
      case ERROR:
        logger.error(message, ex);
        break;
    } 
    MDC.remove("LOG_FILE_NAME");
  }
}
