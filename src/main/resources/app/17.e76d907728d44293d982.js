(window.webpackJsonp=window.webpackJsonp||[]).push([[17],{LS8g:function(e,n,t){"use strict";t.r(n),t.d(n,"RegJudgeEditorPageModule",function(){return g});var i=t("ofXK"),o=t("3Pt+"),r=t("TEn/"),a=t("tyNb"),b=t("cygB"),c=t("fXoL");function d(e,n){if(1&e){var t=c.Lb();c.Kb(0,"ion-content"),c.Kb(1,"form",6,7),c.Sb("ngSubmit",function(){c.ec(t);var e=c.dc(2);return c.Ub().save(e.value)})("keyup.enter",function(){c.ec(t);var e=c.dc(2);return c.Ub().save(e.value)}),c.Kb(3,"ion-list"),c.Kb(4,"ion-item-divider",8),c.kc(5,"Wertungsrichter/-in"),c.Jb(),c.Kb(6,"ion-item"),c.Kb(7,"ion-avatar",0),c.Ib(8,"img",9),c.Jb(),c.Kb(9,"ion-input",10),c.Sb("ngModelChange",function(e){return c.ec(t),c.Ub().registration.name=e}),c.Jb(),c.Kb(10,"ion-input",11),c.Sb("ngModelChange",function(e){return c.ec(t),c.Ub().registration.vorname=e}),c.Jb(),c.Jb(),c.Kb(11,"ion-item"),c.Kb(12,"ion-avatar",0),c.kc(13," \xa0 "),c.Jb(),c.Kb(14,"ion-label",8),c.kc(15,"Geschlecht"),c.Jb(),c.Kb(16,"ion-select",12),c.Sb("ngModelChange",function(e){return c.ec(t),c.Ub().registration.geschlecht=e}),c.Kb(17,"ion-select-option",13),c.kc(18,"weiblich"),c.Jb(),c.Kb(19,"ion-select-option",13),c.kc(20,"m\xe4nnlich"),c.Jb(),c.Jb(),c.Jb(),c.Kb(21,"ion-item-divider",8),c.kc(22,"Kontakt"),c.Jb(),c.Kb(23,"ion-item"),c.Kb(24,"ion-avatar",0),c.Ib(25,"img",14),c.Jb(),c.Ib(26,"ion-input",15),c.Ib(27,"ion-input",16),c.Jb(),c.Kb(28,"ion-item-divider"),c.Kb(29,"ion-avatar",0),c.Ib(30,"img",17),c.Jb(),c.Kb(31,"ion-label"),c.kc(32,"Einteilung"),c.Jb(),c.Jb(),c.Kb(33,"ion-item"),c.Kb(34,"ion-avatar",0),c.kc(35," \xa0 "),c.Jb(),c.Kb(36,"ion-label",8),c.kc(37,"Kommentar"),c.Jb(),c.Kb(38,"ion-textarea",18),c.Sb("ngModelChange",function(e){return c.ec(t),c.Ub().registration.comment=e}),c.Jb(),c.Jb(),c.Jb(),c.Kb(39,"ion-list"),c.Kb(40,"ion-button",19,20),c.Ib(42,"ion-icon",21),c.kc(43,"Speichern"),c.Jb(),c.Jb(),c.Jb(),c.Jb()}if(2&e){var i=c.dc(2),o=c.Ub();c.xb(9),c.ac("disabled",!1)("ngModel",o.registration.name),c.xb(1),c.ac("disabled",!1)("ngModel",o.registration.vorname),c.xb(6),c.ac("disabled",o.registration.athletId>0)("ngModel",o.registration.geschlecht),c.xb(1),c.ac("value","W"),c.xb(2),c.ac("value","M"),c.xb(7),c.ac("readonly",!1)("disabled",!1)("ngModel",o.registration.mail),c.xb(1),c.ac("readonly",!1)("disabled",!1)("ngModel",o.registration.mobilephone),c.xb(11),c.ac("disabled",!1)("ngModel",o.registration.comment),c.xb(2),c.ac("disabled",o.waiting||!i.valid||i.untouched)}}function s(e,n){if(1&e){var t=c.Lb();c.Kb(0,"ion-button",22,23),c.Sb("click",function(){return c.ec(t),c.Ub().delete()}),c.Ib(2,"ion-icon",24),c.kc(3,"Anmeldung l\xf6schen"),c.Jb()}if(2&e){var i=c.Ub();c.ac("disabled",i.waiting)}}var l=[{path:"",component:function(){function e(e,n,t,i,o){this.navCtrl=e,this.route=n,this.backendService=t,this.alertCtrl=i,this.zone=o,this.waiting=!1}return e.prototype.ngOnInit=function(){var e=this;this.waiting=!0,this.wkId=this.route.snapshot.paramMap.get("wkId"),this.regId=parseInt(this.route.snapshot.paramMap.get("regId")),this.judgeId=parseInt(this.route.snapshot.paramMap.get("judgeId")),this.backendService.getCompetitions().subscribe(function(n){var t=n.find(function(n){return n.uuid===e.wkId});e.wettkampfId=parseInt(t.id),e.backendService.loadProgramsForCompetition(t.uuid).subscribe(function(n){e.wkPgms=n,e.judgeId?e.backendService.loadJudgeRegistrations(e.wkId,e.regId).subscribe(function(n){e.updateUI(n.find(function(n){return n.id===e.judgeId}))}):e.updateUI({id:0,vereinregistrationId:e.regId,name:"",vorname:"",mobilephone:"+417",mail:"",comment:"",registrationTime:0})})})},e.prototype.editable=function(){return this.backendService.loggedIn},e.prototype.updateUI=function(e){var n=this;this.zone.run(function(){n.waiting=!1,n.wettkampf=n.backendService.competitionName,n.registration=e})},e.prototype.save=function(e){var n=this,t=Object.assign({},this.registration,e);console.log(t),0===this.judgeId||0===t.id?this.backendService.createJudgeRegistration(this.wkId,this.regId,t).subscribe(function(){n.navCtrl.pop()}):this.backendService.saveJudgeRegistration(this.wkId,this.regId,t).subscribe(function(){n.navCtrl.pop()})},e.prototype.delete=function(){var e=this;this.alertCtrl.create({header:"Achtung",subHeader:"L\xf6schen der Judge-Anmeldung am Wettkampf",message:"Hiermit wird die Anmeldung von "+this.registration.name+", "+this.registration.vorname+" am Wettkampf gel\xf6scht.",buttons:[{text:"ABBRECHEN",role:"cancel",handler:function(){}},{text:"OKAY",handler:function(){e.backendService.deleteJudgeRegistration(e.wkId,e.regId,e.registration).subscribe(function(){e.navCtrl.pop()})}}]}).then(function(e){return e.present()})},e.\u0275fac=function(n){return new(n||e)(c.Hb(r.N),c.Hb(a.a),c.Hb(b.a),c.Hb(r.b),c.Hb(c.A))},e.\u0275cmp=c.Bb({type:e,selectors:[["app-reg-judge-editor"]],decls:14,vars:3,consts:[["slot","start"],["defaultHref","/"],["slot","end"],[1,"judge"],[4,"ngIf"],["size","large","expand","block","color","danger",3,"disabled","click",4,"ngIf"],[3,"ngSubmit","keyup.enter"],["judgeRegistrationForm","ngForm"],["color","primary"],["src","assets/imgs/chief.png"],["required","","placeholder","Name","type","text","name","name","required","",3,"disabled","ngModel","ngModelChange"],["required","","placeholder","Vorname","type","text","name","vorname","required","",3,"disabled","ngModel","ngModelChange"],["required","","placeholder","Geschlecht","name","geschlecht","okText","Okay","cancelText","Abbrechen","required","",3,"disabled","ngModel","ngModelChange"],[3,"value"],["src","assets/imgs/atmail.png"],["placeholder","Mail","type","email","pattern","^[_a-z0-9-]+(\\.[_a-z0-9-]+)*@[a-z0-9-]+(\\.[a-z0-9-]+)*(\\.[a-z]{2,5})$","name","mail","required","",3,"readonly","disabled","ngModel"],["placeholder","Mobile/SMS (+4179...)","type","tel","pattern","[+]{1}[0-9]{11,14}","name","mobilephone","required","",3,"readonly","disabled","ngModel"],["src","assets/imgs/wettkampf.png"],["placeholder","Kommentar zur Einteilung als Wertungsrichter","name","comment",3,"disabled","ngModel","ngModelChange"],["size","large","expand","block","type","submit","color","success",3,"disabled"],["btnSaveNext",""],["slot","start","name",""],["size","large","expand","block","color","danger",3,"disabled","click"],["btnDelete",""],["slot","start","name","trash"]],template:function(e,n){1&e&&(c.Kb(0,"ion-header"),c.Kb(1,"ion-toolbar"),c.Kb(2,"ion-buttons",0),c.Ib(3,"ion-back-button",1),c.Jb(),c.Kb(4,"ion-title"),c.kc(5,"Wertungsrichter-Anmeldung"),c.Jb(),c.Kb(6,"ion-note",2),c.Kb(7,"div",3),c.kc(8),c.Jb(),c.Jb(),c.Jb(),c.Jb(),c.jc(9,d,44,17,"ion-content",4),c.Kb(10,"ion-footer"),c.Kb(11,"ion-toolbar"),c.Kb(12,"ion-list"),c.jc(13,s,4,1,"ion-button",5),c.Jb(),c.Jb(),c.Jb()),2&e&&(c.xb(8),c.mc("f\xfcr ",n.wettkampf,""),c.xb(1),c.ac("ngIf",n.registration),c.xb(4),c.ac("ngIf",n.judgeId>0))},directives:[r.n,r.J,r.h,r.e,r.f,r.I,r.z,i.m,r.l,r.w,r.k,o.j,o.e,o.f,r.r,r.q,r.d,r.p,r.R,o.i,o.d,o.g,r.v,r.D,r.Q,r.E,o.h,r.H,r.g,r.o],styles:[""]}),e}()}],u=function(){function e(){}return e.\u0275mod=c.Fb({type:e}),e.\u0275inj=c.Eb({factory:function(n){return new(n||e)},imports:[[a.i.forChild(l)],a.i]}),e}(),g=function(){function e(){}return e.\u0275mod=c.Fb({type:e}),e.\u0275inj=c.Eb({factory:function(n){return new(n||e)},imports:[[i.c,o.a,r.K,u]]}),e}()}}]);