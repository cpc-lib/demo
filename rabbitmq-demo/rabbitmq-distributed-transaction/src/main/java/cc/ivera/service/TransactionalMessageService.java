package cc.ivera.service;


import cc.ivera.support.binding.Destination;
import cc.ivera.support.message.TxMessage;

/**
 * @version v1.0
 * @description
 * @since 2020/2/3 9:59
 */
public interface TransactionalMessageService {

    void sendTransactionalMessage(Destination destination, TxMessage message);
}
