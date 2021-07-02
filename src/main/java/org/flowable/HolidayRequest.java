package org.flowable;

import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class HolidayRequest {
    public static void main(String[] args) {

        /**
         * 由ProcessEngineConfiguration实例创建并初始化ProcessEngine流程引擎实例
         */
        ProcessEngineConfiguration cfg = new
                StandaloneProcessEngineConfiguration()
                .setJdbcUrl("jdbc:mysql://localhost:3306/holidayrequest?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai")
                .setJdbcUsername("root")
                .setJdbcPassword("123456")
                .setJdbcDriver("com.mysql.cj.jdbc.Driver")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);

        ProcessEngine processEngine = cfg.buildProcessEngine();

        /**
         * 这样就得到了一个启动可用的流程引擎。接下来为它提供一个流程！
         */
        RepositoryService repositoryService = processEngine.getRepositoryService();
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("holiday-request.bpmn20.xml")
                .deploy();

        /**
         * 我们现在可以通过API查询验证流程定义已经部署在引擎中（并学习一些API）。
         * 通过RepositoryService创建的ProcessDefinitionQuery对象实现。
         */

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();
        System.out.println("Found process definition : " + processDefinition.getName());


        /**
         * 要启动流程实例，需要提供一些初始化流程变量。一般来说，可以通过呈现给用户的表单，
         * 或者在流程由其他系统自动触发时通过REST API，来获取这些变量。
         * 在这个例子里，我们简化为使用java.util.Scanner类在命令行输入一些数据：
         */
        Scanner scanner= new Scanner(System.in);

        System.out.println("Who are you?");
        String employee = scanner.nextLine();

        System.out.println("How many holidays do you want to request?");
        Integer nrOfHolidays = Integer.valueOf(scanner.nextLine());

        System.out.println("Why do you need them?");
        String description = scanner.nextLine();


        /**
         * 接下来，我们使用RuntimeService启动一个流程实例。
         * 收集的数据作为一个java.util.Map实例传递，其中的键就是之后用于获取变量的标识符。
         * 这个流程实例使用key启动。这个key就是BPMN 2.0 XML文件中设置的id属性，
         * 在这个例子里是holidayRequest。
         *
         * （请注意：除了使用key之外，在后面你还会看到有很多其他方式启动一个流程实例）
         */
        RuntimeService runtimeService = processEngine.getRuntimeService();

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("employee", employee);
        variables.put("nrOfHolidays", nrOfHolidays);
        variables.put("description", description);
        ProcessInstance processInstance =
                runtimeService.startProcessInstanceByKey("holidayRequest", variables);

        /**
         * 要获得实际的任务列表，需要通过TaskService创建一个TaskQuery。
         * 我们配置这个查询只返回’managers’组的任务：
         */
        TaskService taskService = processEngine.getTaskService();
        List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup("managers").list();
        System.out.println("You have " + tasks.size() + " tasks:");
        for (int i=0; i<tasks.size(); i++) {
            System.out.println((i+1) + ") " + tasks.get(i).getName());
        }

        /**
         * 可以使用任务Id获取特定流程实例的变量，并在屏幕上显示实际的申请：
         */
        System.out.println("Which task would you like to complete?");
        int taskIndex = Integer.valueOf(scanner.nextLine());
        Task task = tasks.get(taskIndex - 1);
        Map<String, Object> processVariables = taskService.getVariables(task.getId());
        System.out.println(processVariables.get("employee") + " wants " +
                processVariables.get("nrOfHolidays") + " of holidays. Do you approve this?");

        /**
         * 经理现在就可以完成任务了。在现实中，这通常意味着由用户提交一个表单。
         * 表单中的数据作为流程变量传递。
         * 在这里，我们在完成任务时传递带有’approved’变量
         * （这个名字很重要，因为之后会在顺序流的条件中使用！）的map来模拟：
         */
        boolean approved = scanner.nextLine().toLowerCase().equals("y");
        variables = new HashMap<String, Object>();
        variables.put("approved", approved);
        taskService.complete(task.getId(), variables);

        /**
         * 选择使用Flowable这样的流程引擎的原因之一，
         * 是它可以自动存储所有流程实例的审计数据或历史数据。
         * 这些数据可以用于创建报告，深入展现组织运行的情况，瓶颈在哪里，等等。
         */

        /**
         * 例如，如果希望显示流程实例已经执行的时间，就可以从ProcessEngine
         * 获取HistoryService，
         * 并创建历史活动(historical activities)的查询。在下面的代码片段中，
         * 可以看到我们添加了一些额外的过滤条件：
         *
         *      只选择一个特定流程实例的活动
         *
         *      只选择已完成的活动
         *
         * 结果按照结束时间排序，代表其执行顺序。
         */
        HistoryService historyService = processEngine.getHistoryService();
        List<HistoricActivityInstance> activities =
                historyService.createHistoricActivityInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .finished()
                        .orderByHistoricActivityInstanceEndTime().asc()
                        .list();

        for (HistoricActivityInstance activity : activities) {
            System.out.println(activity.getActivityId() + " took "
                    + activity.getDurationInMillis() + " milliseconds");
        }

    }
}
