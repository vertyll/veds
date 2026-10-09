# Access policy

How project-service decides what a person may do with a project.

`ProjectAccessPolicy` is a pure domain service: no Spring, no repositories, no security context.
It evaluates rules in order and the first decision wins, so **deny overrides**:

1. `RESOURCE_STATE` — an archived project is frozen for everyone, including its owner
2. `OWNER_GRANT`
3. `PUBLIC_VISIBILITY` — read only
4. `ROLE_GRANT`

The order is the rule. Putting `OWNER_GRANT` first would make an owner able to edit an archived
project, which is exactly what archiving is supposed to prevent.

`permissionsOf` must agree with `permits` for every subject, and a test enforces that: the front
end renders its controls from the first, so a drift would offer actions the policy then refuses.
