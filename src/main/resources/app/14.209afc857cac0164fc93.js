(window.webpackJsonp=window.webpackJsonp||[]).push([[14],{SFjQ:function(e,t,n){"use strict";n.r(t),n.d(t,"ClubregEditorPageModule",(function(){return h}));var i=n("ofXK"),r=n("3Pt+"),a=n("tyNb"),o=n("TEn/"),s=n("mrSG"),c=(n("ppBR"),n("cygB")),d=n("IzEk"),l=n("HC5s"),b=n("fXoL");function u(e,t){if(1&e&&(b.Kb(0,"ion-item"),b.Kb(1,"ion-avatar",0),b.Ib(2,"img",23),b.Jb(),b.Ib(3,"ion-input",24),b.Ib(4,"ion-input",25),b.Jb()),2&e){var n=b.Ub(2);b.xb(3),b.ac("readonly",!1)("disabled",!1)("ngModel",n.registration.secret),b.xb(1),b.ac("readonly",!1)("disabled",!1)("ngModel",n.registration.verification)}}function p(e,t){if(1&e&&(b.Kb(0,"ion-item"),b.kc(1),b.Jb()),2&e){var n=t.$implicit;b.xb(1),b.mc(" ",n," ")}}function g(e,t){if(1&e){var n=b.Lb();b.Kb(0,"ion-content"),b.Kb(1,"form",7,8),b.Sb("ngSubmit",(function(){b.ec(n);var e=b.dc(2);return b.Ub().save(e.value)}))("keyup.enter",(function(){b.ec(n);var e=b.dc(2);return b.Ub().save(e.value)})),b.Kb(3,"ion-list"),b.Kb(4,"ion-item-divider",9),b.kc(5,"Verein"),b.Jb(),b.Kb(6,"ion-item"),b.Kb(7,"ion-avatar",0),b.Ib(8,"img",10),b.Jb(),b.Ib(9,"ion-input",11),b.Ib(10,"ion-input",12),b.Jb(),b.Kb(11,"ion-item-divider",9),b.kc(12,"Verantwortliche Person"),b.Jb(),b.Kb(13,"ion-item"),b.Kb(14,"ion-avatar",0),b.Ib(15,"img",13),b.Jb(),b.Ib(16,"ion-input",14),b.Ib(17,"ion-input",15),b.Jb(),b.jc(18,u,5,6,"ion-item",4),b.Kb(19,"ion-item-divider",9),b.kc(20,"Kontakt"),b.Jb(),b.Kb(21,"ion-item"),b.Kb(22,"ion-avatar",0),b.Ib(23,"img",16),b.Jb(),b.Ib(24,"ion-input",17),b.Ib(25,"ion-input",18),b.Jb(),b.Jb(),b.Kb(26,"ion-button",19,20),b.Ib(28,"ion-icon",21),b.kc(29,"Speichern "),b.Jb(),b.Jb(),b.Kb(30,"ion-list"),b.Kb(31,"ion-item-divider",9),b.kc(32),b.Jb(),b.jc(33,p,2,1,"ion-item",22),b.Jb(),b.Jb()}if(2&e){var i=b.dc(2),r=b.Ub();b.xb(9),b.ac("readonly",!1)("disabled",!1)("ngModel",r.registration.vereinname),b.xb(1),b.ac("readonly",!1)("disabled",!1)("ngModel",r.registration.verband),b.xb(6),b.ac("readonly",!1)("disabled",!1)("ngModel",r.registration.respVorname),b.xb(1),b.ac("readonly",!1)("disabled",!1)("ngModel",r.registration.respName),b.xb(1),b.ac("ngIf",0===r.regId),b.xb(6),b.ac("readonly",!1)("disabled",!1)("ngModel",r.registration.mail),b.xb(1),b.ac("readonly",!1)("disabled",!1)("ngModel",r.registration.mobilephone),b.xb(1),b.ac("disabled",r.waiting||!i.valid||i.untouched),b.xb(6),b.mc("Pendente Verarbeitungen (",r.sSyncActions.length,")"),b.xb(1),b.ac("ngForOf",r.sSyncActions)}}var m=[{path:"",component:function(){function e(e,t,n,i,r,a){this.navCtrl=e,this.route=t,this.backendService=n,this.alertCtrl=i,this.actionSheetController=r,this.zone=a,this.waiting=!1,this.sSyncActions=[]}return e.prototype.ngOnInit=function(){var e=this;this.waiting=!0,this.wkId=this.route.snapshot.paramMap.get("wkId"),this.regId=parseInt(this.route.snapshot.paramMap.get("regId")),this.backendService.getCompetitions().subscribe((function(t){e.wettkampfId=parseInt(t.find((function(t){return t.uuid===e.wkId})).id),e.regId?e.backendService.getClubRegistrations(e.wkId).subscribe((function(t){e.updateUI(t.find((function(t){return t.id===e.regId})))})):e.updateUI({mobilephone:"+417"})}))},e.prototype.presentActionSheet=function(){return Object(s.b)(this,void 0,void 0,(function(){var e=this;return Object(s.d)(this,(function(t){switch(t.label){case 0:return[4,this.actionSheetController.create(this.registration.vereinId?{header:"Anmeldungen",cssClass:"my-actionsheet-class",buttons:[{text:"Athlet & Athletinnen",icon:"list",handler:function(){e.editAthletRegistrations()}},{text:"Wertungsrichter",icon:"glasses",handler:function(){e.editJudgeRegistrations()}},{text:"Kopieren aus anderem Wettkampf",icon:"share",handler:function(){e.copyFromOthers()}},{text:"Registrierung l\xf6schen",role:"destructive",icon:"trash",handler:function(){e.delete()}},{text:"Abbrechen",icon:"close",role:"cancel",handler:function(){console.log("Cancel clicked")}}]}:{header:"Anmeldungen",cssClass:"my-actionsheet-class",buttons:[{text:"Athlet & Athletinnen",icon:"list",handler:function(){e.editAthletRegistrations()}},{text:"Wertungsrichter",icon:"glasses",handler:function(){e.editJudgeRegistrations()}},{text:"Registrierung l\xf6schen",role:"destructive",icon:"trash",handler:function(){e.delete()}},{text:"Abbrechen",icon:"close",role:"cancel",handler:function(){console.log("Cancel clicked")}}]})];case 1:return[4,t.sent().present()];case 2:return t.sent(),[2]}}))}))},e.prototype.editable=function(){return this.backendService.loggedIn},e.prototype.ionViewWillEnter=function(){this.getSyncActions()},e.prototype.getSyncActions=function(){var e=this;this.regId>0&&this.backendService.loadRegistrationSyncActions().pipe(Object(d.a)(1)).subscribe((function(t){e.sSyncActions=t.filter((function(t){return t.verein.id===e.registration.id})).map((function(e){return e.caption}))}))},e.prototype.updateUI=function(e){var t=this;this.zone.run((function(){t.waiting=!1,t.wettkampf=t.backendService.competitionName,t.registration=e,t.registration.mail.length>1&&(t.backendService.currentUserName=t.registration.mail)}))},e.prototype.save=function(e){var t=this;if(0===this.regId){var n=e;n.secret!==n.verification?this.alertCtrl.create({header:"Achtung",subHeader:"Passwort-Verifikation",message:"Das Passwort stimmt nicht mit der Verifikation \xfcberein. Beide m\xfcssen genau gleich erfasst sein.",buttons:[{text:"OKAY",role:"cancel",handler:function(){}}]}).then((function(e){return e.present()})):this.backendService.createClubRegistration(this.wkId,{mail:n.mail,mobilephone:n.mobilephone,respName:n.respName,respVorname:n.respVorname,verband:n.verband,vereinname:n.vereinname,wettkampfId:n.wettkampfId||this.wettkampfId,secret:n.secret}).subscribe((function(e){t.regId=e.id,t.registration=e,t.backendService.currentUserName=e.mail,t.editAthletRegistrations()}))}else this.backendService.saveClubRegistration(this.wkId,{id:this.registration.id,registrationTime:this.registration.registrationTime,vereinId:this.registration.vereinId,mail:e.mail,mobilephone:e.mobilephone,respName:e.respName,respVorname:e.respVorname,verband:e.verband,vereinname:e.vereinname,wettkampfId:e.wettkampfId||this.wettkampfId})},e.prototype.delete=function(){var e=this;this.alertCtrl.create({header:"Achtung",subHeader:"L\xf6schen der Vereins-Registrierung und dessen Anmeldungen am Wettkampf",message:"Hiermit wird die Vereinsanmeldung am Wettkampf komplett gel\xf6scht.",buttons:[{text:"ABBRECHEN",role:"cancel",handler:function(){}},{text:"OKAY",handler:function(){e.backendService.deleteClubRegistration(e.wkId,e.regId).subscribe((function(){e.navCtrl.pop()}))}}]}).then((function(e){return e.present()}))},e.prototype.editAthletRegistrations=function(){this.navCtrl.navigateForward("reg-athletlist/"+this.backendService.competition+"/"+this.regId)},e.prototype.editJudgeRegistrations=function(){this.navCtrl.navigateForward("reg-judgelist/"+this.backendService.competition+"/"+this.regId)},e.prototype.copyFromOthers=function(){var e=this;this.backendService.findCompetitionsByVerein(this.registration.vereinId).subscribe((function(t){var n=t.map((function(e){return{type:"radio",label:e.titel+" ("+Object(l.d)(e.datum)+")",value:e}}));e.alertCtrl.create({header:"Anmeldungen kopieren",cssClass:"my-optionselection-class",message:"Aus welchem Wettkampf sollen die Anmeldungen kopiert werden?",inputs:n,buttons:[{text:"ABBRECHEN",role:"cancel",handler:function(){}},{text:"OKAY",handler:function(t){e.backendService.copyClubRegsFromCompetition(t.uuid,e.backendService.competition,e.registration.id).subscribe((function(){e.getSyncActions(),e.alertCtrl.create({header:"Anmeldungen kopieren",message:"Alle nicht bereits erfassten Anmeldungen wurden \xfcbernommen.",buttons:[{text:"OKAY",role:"cancel",handler:function(){}},{text:"> Athlet-Anmeldungen",handler:function(){e.editAthletRegistrations()}},{text:"> WR Anmeldungen",handler:function(){e.editJudgeRegistrations()}}]}).then((function(e){return e.present()}))}))}}]}).then((function(e){return e.present()}))}))},e.\u0275fac=function(t){return new(t||e)(b.Hb(o.N),b.Hb(a.a),b.Hb(c.a),b.Hb(o.b),b.Hb(o.a),b.Hb(b.z))},e.\u0275cmp=b.Bb({type:e,selectors:[["app-clubreg-editor"]],decls:15,vars:3,consts:[["slot","start"],["defaultHref","/"],["slot","end"],[1,"athlet"],[4,"ngIf"],["size","large","expand","block","color","primary",3,"disabled","click"],["slot","start","name","list"],[3,"ngSubmit","keyup.enter"],["clubRegistrationform","ngForm"],["color","primary"],["src","assets/imgs/verein.png"],["placeholder","Vereinsname","type","text","name","vereinname","required","",3,"readonly","disabled","ngModel"],["placeholder","Verband","type","text","name","verband","required","",3,"readonly","disabled","ngModel"],["src","assets/imgs/chief.png"],["placeholder","Vorname","type","text","name","respVorname","required","",3,"readonly","disabled","ngModel"],["placeholder","Nachname","type","text","name","respName","required","",3,"readonly","disabled","ngModel"],["src","assets/imgs/atmail.png"],["placeholder","Mail","type","email","pattern","^[_a-z0-9-]+(\\.[_a-z0-9-]+)*@[a-z0-9-]+(\\.[a-z0-9-]+)*(\\.[a-z]{2,5})$","name","mail","required","",3,"readonly","disabled","ngModel"],["placeholder","Mobile/SMS (+4179...)","type","tel","pattern","[+]{1}[0-9]{11,14}","name","mobilephone","required","",3,"readonly","disabled","ngModel"],["size","large","expand","block","type","submit","color","success",3,"disabled"],["btnSaveNext",""],["slot","start","name","save"],[4,"ngFor","ngForOf"],["src","assets/imgs/password.png"],["placeholder","Passwort","type","password","name","secret","required","",3,"readonly","disabled","ngModel"],["placeholder","Verification","type","password","name","verification","required","",3,"readonly","disabled","ngModel"]],template:function(e,t){1&e&&(b.Kb(0,"ion-header"),b.Kb(1,"ion-toolbar"),b.Kb(2,"ion-buttons",0),b.Ib(3,"ion-back-button",1),b.Jb(),b.Kb(4,"ion-title"),b.kc(5,"Vereins-Anmeldung"),b.Jb(),b.Kb(6,"ion-note",2),b.Kb(7,"div",3),b.kc(8),b.Jb(),b.Jb(),b.Jb(),b.Jb(),b.jc(9,g,34,22,"ion-content",4),b.Kb(10,"ion-footer"),b.Kb(11,"ion-toolbar"),b.Kb(12,"ion-button",5),b.Sb("click",(function(){return t.presentActionSheet()})),b.Ib(13,"ion-icon",6),b.kc(14,"Anmeldungen... "),b.Jb(),b.Jb(),b.Jb()),2&e&&(b.xb(8),b.mc("f\xfcr ",t.wettkampf,""),b.xb(1),b.ac("ngIf",t.registration),b.xb(3),b.ac("disabled",t.waiting||0===t.regId))},directives:[o.n,o.J,o.h,o.e,o.f,o.I,o.z,i.m,o.l,o.g,o.o,o.k,r.j,r.e,r.f,o.w,o.r,o.q,o.d,o.p,o.R,r.i,r.d,r.g,r.h,i.l],styles:[""]}),e}()}],h=function(){function e(){}return e.\u0275mod=b.Fb({type:e}),e.\u0275inj=b.Eb({factory:function(t){return new(t||e)},imports:[[i.c,r.a,o.K,a.i.forChild(m)]]}),e}()}}]);