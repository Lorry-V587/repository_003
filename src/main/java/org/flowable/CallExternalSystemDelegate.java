package org.flowable;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * 在现实中，这个逻辑可以做任何事情：向某个系统发起一个HTTP REST服务调用，
 * 或调用某个使用了好几十年的系统中的遗留代码。
 * 我们不会在这里实现实际的逻辑，而只是简单的日志记录流程。
 */
public class CallExternalSystemDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution delegateExecution) {
        System.out.println("Calling the external system for employee "
                + delegateExecution.getVariable("employee"));
    }
}
