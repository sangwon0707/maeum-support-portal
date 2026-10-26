package kr.co.maeum.support.ocr;

import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/** 응답을 모두 받기 전에 용량을 제한해 내부 API 이상에도 메모리가 늘어나지 않게 한다. */
final class LimitedBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {
    private final HttpResponse.BodySubscriber<byte[]> delegate =
            HttpResponse.BodySubscribers.ofByteArray();
    private Flow.Subscription subscription;
    private long received;

    public CompletionStage<byte[]> getBody() {
        return delegate.getBody();
    }

    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        delegate.onSubscribe(subscription);
    }

    public void onNext(List<ByteBuffer> buffers) {
        received += buffers.stream().mapToLong(ByteBuffer::remaining).sum();
        if (received > 1024 * 1024) {
            subscription.cancel();
            delegate.onError(new OcrProcessingException("RESPONSE_TOO_LARGE", false));
        } else {
            delegate.onNext(buffers);
        }
    }

    public void onError(Throwable error) {
        delegate.onError(error);
    }

    public void onComplete() {
        delegate.onComplete();
    }
}
