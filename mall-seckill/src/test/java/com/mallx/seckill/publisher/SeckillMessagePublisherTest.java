package com.mallx.seckill.publisher;

import com.mallx.seckill.constant.SeckillMqNames;
import com.mallx.seckill.message.SeckillOrderMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(OutputCaptureExtension.class)
class SeckillMessagePublisherTest {
    private RabbitTemplate rabbitTemplate;
    private SeckillMessagePublisher publisher;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        publisher = new SeckillMessagePublisher(rabbitTemplate);
    }

    @Test
    void usesUniqueCorrelationIdForEachDelivery() {
        SeckillOrderMessage message = message("REQ-001");

        publisher.send(message);
        publisher.send(message);

        List<CorrelationData> correlationDataList = captureCorrelationData(message, 2);
        assertThat(correlationDataList)
                .extracting(CorrelationData::getId)
                .allMatch(id -> id.startsWith("REQ-001:"));
        assertThat(correlationDataList.get(0).getId())
                .isNotEqualTo(correlationDataList.get(1).getId());
    }

    @Test
    void logsNackAndLeavesRequestForRecovery(CapturedOutput output) {
        SeckillOrderMessage message = message("REQ-NACK");
        publisher.send(message);
        CorrelationData correlationData = captureCorrelationData(message, 1).get(0);

        correlationData.getFuture().complete(
                new CorrelationData.Confirm(false, "broker rejected"));

        assertThat(output).contains("秒杀消息被 RabbitMQ 拒绝")
                .contains("requestNo=REQ-NACK")
                .contains("broker rejected");
    }

    @Test
    void logsReturnedMessageAfterBrokerAck(CapturedOutput output) {
        SeckillOrderMessage message = message("REQ-RETURN");
        publisher.send(message);
        CorrelationData correlationData = captureCorrelationData(message, 1).get(0);
        correlationData.setReturned(new ReturnedMessage(
                new Message(new byte[0]),
                312,
                "NO_ROUTE",
                SeckillMqNames.ORDER_EXCHANGE,
                SeckillMqNames.ORDER_ROUTING_KEY));

        correlationData.getFuture().complete(new CorrelationData.Confirm(true, null));

        assertThat(output).contains("秒杀消息无法路由")
                .contains("requestNo=REQ-RETURN")
                .contains("replyCode=312")
                .contains("NO_ROUTE");
    }

    private List<CorrelationData> captureCorrelationData(
            SeckillOrderMessage message, int invocationCount) {
        ArgumentCaptor<CorrelationData> captor = ArgumentCaptor.forClass(CorrelationData.class);
        verify(rabbitTemplate, times(invocationCount)).convertAndSend(
                eq(SeckillMqNames.ORDER_EXCHANGE),
                eq(SeckillMqNames.ORDER_ROUTING_KEY),
                eq(message),
                captor.capture());
        return captor.getAllValues();
    }

    private SeckillOrderMessage message(String requestNo) {
        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setRequestNo(requestNo);
        return message;
    }
}
