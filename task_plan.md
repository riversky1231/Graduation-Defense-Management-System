# Task Plan

## Goal
在不破坏现有功能和测试的前提下，继续推进大文件拆分，优先完成低耦合、可验证的控制器与服务层切口。

## Phases
| Phase | Status | Notes |
|---|---|---|
| 检查当前状态 | complete | 已确认工作区脏状态和半拆分的 `AdminUserController` |
| 选择拆分切口 | complete | 已完成 `AdminController`、`StudentController` 与 `ExportController` 的低耦合切口选择 |
| 实施拆分 | complete | 用户管理迁入 `AdminUserController`，学生导入迁入 `StudentImportController`，导出角色端点与小组导出端点迁入独立控制器，教师侧学生接口迁入 `TeacherStudentController` |
| 验证 | complete | 五轮拆分后均已通过编译和全量测试 |
| 服务层拆分 | complete | `ScoreServiceImpl` 的小组统分/调节系数/候选人等重逻辑迁入 `ScoreGroupSupport`，并保留批量查询主路径 |
| 评分控制器拆分 | complete | `largegroup` 端点迁入 `LargeGroupScoreController`，原 URL 保持不变 |
| 分组控制器拆分 | complete | `GroupTeacherController` 中批量分配、配置与随机分配端点迁入 `GroupAssignmentController` |

## Errors Encountered
| Error | Attempt | Resolution |
|---|---|---|
| `CreateProcessAsUserW failed: 5` | 1 | 改为申请受限外执行检查仓库状态 |
| PowerShell 将 `-Dtest=A,B` 解析成参数列表 | 1 | 改为 `mvn -q "-Dtest=A,B" test` |
| 新控制器漏掉 `RequestParam` 导入 | 1 | 补齐导入后重新编译 |
