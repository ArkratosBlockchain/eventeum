package net.consensys.eventeum.integration.broadcast.blockchain;

import net.consensys.eventeum.dto.block.BlockDetails;
import net.consensys.eventeum.dto.event.ContractEventDetails;
import net.consensys.eventeum.dto.transaction.TransactionDetails;
import net.consensys.eventeum.integration.broadcast.BroadcastException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import net.consensys.eventeum.integration.SqsSettings;

import net.consensys.eventeum.dto.message.BlockEvent;
import net.consensys.eventeum.dto.message.ContractEvent;
import net.consensys.eventeum.dto.message.EventeumMessage;
import net.consensys.eventeum.dto.message.TransactionEvent;
import net.consensys.eventeum.utils.JSON;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.regions.Regions;

/**
 * A BlockchainEventBroadcaster that broadcasts the events via a http post.
 *
 * The url to post to for block and contract events can be configured via the
 * broadcast.http.contractEvents and broadcast.http.blockEvents properties.
 *
 * @author Craig Williams <craig.williams@consensys.net>
 */
public class SqsBlockchainEventBroadcaster implements BlockchainEventBroadcaster {

    private SqsSettings settings;

    private RetryTemplate retryTemplate;

    private AmazonSQS sqs;

    public SqsBlockchainEventBroadcaster(SqsSettings settings, RetryTemplate retryTemplate) {

        this.settings = settings;
        this.sqs = AmazonSQSClientBuilder
            .standard()
            .withRegion(Regions.fromName(this.settings.getAwsRegion()))
            .build();

        this.retryTemplate = retryTemplate;
    }

    @Override
    public void broadcastNewBlock(BlockDetails block) {

        if (this.settings.isBlockMonitoringEnabled()) {
            final EventeumMessage<BlockDetails> message = createBlockEventMessage(block);

            retryTemplate.execute((context) -> {
                SendMessageRequest send_msg_request = new SendMessageRequest()
                    .withQueueUrl(this.settings.getBlockEndpointUrl())
                    .withMessageGroupId("new_block")
                    .withMessageDeduplicationId("new_block_" + block.getHash())
                    .withMessageBody(JSON.stringify(message))
                    ;
                sqs.sendMessage(send_msg_request);

                return null;
            });
        }
    }

    @Override
    public void broadcastContractEvent(ContractEventDetails eventDetails) {

        final EventeumMessage<ContractEventDetails> message = createContractEventMessage(eventDetails);

        retryTemplate.execute((context) -> {
            SendMessageRequest send_msg_request = new SendMessageRequest()
                .withQueueUrl(this.settings.getEventEndpointUrl())
                .withMessageGroupId("new_event")
                .withMessageDeduplicationId("new_event_" + eventDetails.getTransactionHash())
                .withMessageBody(JSON.stringify(message))
                ;
            sqs.sendMessage(send_msg_request);

            return null;
        });
    }

    @Override
    public void broadcastTransaction(TransactionDetails transactionDetails) {

        final EventeumMessage<TransactionDetails> message = createTransactionEventMessage(transactionDetails);

        retryTemplate.execute((context) -> {
            SendMessageRequest send_msg_request = new SendMessageRequest()
                .withQueueUrl(this.settings.getTxEndpointUrl())
                .withMessageGroupId("new_tx")
                .withMessageDeduplicationId("new_tx_" + transactionDetails.getHash())
                .withMessageBody(JSON.stringify(message))
                ;
            sqs.sendMessage(send_msg_request);

            return null;
        });
    }

    protected EventeumMessage<BlockDetails> createBlockEventMessage(BlockDetails blockDetails) {
        return new BlockEvent(blockDetails);
    }

    protected EventeumMessage<ContractEventDetails> createContractEventMessage(ContractEventDetails contractEventDetails) {
        return new ContractEvent(contractEventDetails);
    }

    protected EventeumMessage<TransactionDetails> createTransactionEventMessage(TransactionDetails transactionDetails) {
        return new TransactionEvent(transactionDetails);
    }

}
