# KeycloakStuff

Мой плагин для keycloak, с недостающим мне функционалом. На данный момент включает в себя:

* Аунтефикатор `idp-find-existing-broker-user` - Полная копия `idp-detect-existing-broker-user`, за исключение того, что
  вместо ошибки, просто пропускает этап
* Валидатор `cooldown-validator` - Позволяет ограничить частоту смены какого либо поля пользователем, при этом администратор ограничению не подвергается 
    * Для локализации надо добавить строку локали с id `spliterash.validation.cooldown-validation-error` в
      переопределения локализации в Realm -> Localization -> Realm overrides
      
      Например: 'Вы сможете обновить {{0}} только через {{1}} секунд'
* Расширение ресурса пользовательских аккаунтов
  * `DELETE` `https://example.com/realms/{realm}/spliterash-account-extension/applications/{client}/access`
  
    Отозвать доступ к сессии у внутреннего клиента (ПОЧЕМУ ЭТОГО НЕТ В КОРОБКЕ ???)