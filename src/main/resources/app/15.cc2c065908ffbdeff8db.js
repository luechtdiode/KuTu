(window.webpackJsonp=window.webpackJsonp||[]).push([[15],{mvXw:function(e,t,n){"use strict";n.r(t),n.d(t,"RegAthletEditorPageModule",function(){return m});var i=n("ofXK"),r=n("3Pt+"),a=n("tyNb"),o=n("TEn/"),b=n("cygB"),l=n("HC5s"),c=n("fXoL");function s(e,t){if(1&e&&(c.Lb(0,"ion-select-option",15),c.Lb(1,"ion-avatar",2),c.Jb(2,"img",8),c.Kb(),c.Lb(3,"ion-label"),c.lc(4),c.Jb(5,"br"),c.Lb(6,"small"),c.lc(7),c.Wb(8,"date"),c.Kb(),c.Kb(),c.Kb()),2&e){var n=t.$implicit;c.bc("value",n.athletId),c.yb(4),c.pc("",n.name,", ",n.vorname," (",n.geschlecht,")"),c.yb(3),c.nc("(",c.Xb(8,5,n.gebdat),")")}}function d(e,t){if(1&e){var n=c.Mb();c.Lb(0,"ion-select",22),c.Tb("ngModelChange",function(e){return c.fc(n),c.Vb(2).selectedClubAthletId=e}),c.kc(1,s,9,7,"ion-select-option",18),c.Kb()}if(2&e){var i=c.Vb(2);c.bc("ngModel",i.selectedClubAthletId),c.yb(1),c.bc("ngForOf",i.clubAthletList)}}function g(e,t){if(1&e&&(c.Lb(0,"ion-select-option",15),c.lc(1),c.Kb()),2&e){var n=t.$implicit;c.bc("value",n.id),c.yb(1),c.mc(n.name)}}function u(e,t){if(1&e){var n=c.Mb();c.Lb(0,"ion-content"),c.Lb(1,"form",6,7),c.Tb("ngSubmit",function(){c.fc(n);var e=c.ec(2);return c.Vb().save(e.value)})("keyup.enter",function(){c.fc(n);var e=c.ec(2);return c.Vb().save(e.value)}),c.Lb(3,"ion-list"),c.Lb(4,"ion-item-divider"),c.Lb(5,"ion-avatar",0),c.Jb(6,"img",8),c.Kb(),c.Lb(7,"ion-label"),c.lc(8,"Athlet / Athletin"),c.Kb(),c.kc(9,d,2,2,"ion-select",9),c.Kb(),c.Lb(10,"ion-item"),c.Lb(11,"ion-avatar",0),c.lc(12," \xa0 "),c.Kb(),c.Lb(13,"ion-input",10),c.Tb("ngModelChange",function(e){return c.fc(n),c.Vb().registration.name=e}),c.Kb(),c.Lb(14,"ion-input",11),c.Tb("ngModelChange",function(e){return c.fc(n),c.Vb().registration.vorname=e}),c.Kb(),c.Kb(),c.Lb(15,"ion-item"),c.Lb(16,"ion-avatar",0),c.lc(17," \xa0 "),c.Kb(),c.Lb(18,"ion-label",12),c.lc(19,"Geburtsdatum"),c.Kb(),c.Lb(20,"ion-input",13),c.Tb("ngModelChange",function(e){return c.fc(n),c.Vb().registration.gebdat=e}),c.Kb(),c.Kb(),c.Lb(21,"ion-item"),c.Lb(22,"ion-avatar",0),c.lc(23," \xa0 "),c.Kb(),c.Lb(24,"ion-label",12),c.lc(25,"Geschlecht"),c.Kb(),c.Lb(26,"ion-select",14),c.Tb("ngModelChange",function(e){return c.fc(n),c.Vb().registration.geschlecht=e}),c.Lb(27,"ion-select-option",15),c.lc(28,"weiblich"),c.Kb(),c.Lb(29,"ion-select-option",15),c.lc(30,"m\xe4nnlich"),c.Kb(),c.Kb(),c.Kb(),c.Lb(31,"ion-item-divider"),c.Lb(32,"ion-avatar",0),c.Jb(33,"img",16),c.Kb(),c.Lb(34,"ion-label"),c.lc(35,"Einteilung"),c.Kb(),c.Kb(),c.Lb(36,"ion-item"),c.Lb(37,"ion-avatar",0),c.lc(38," \xa0 "),c.Kb(),c.Lb(39,"ion-label",12),c.lc(40,"Programm/Kategorie"),c.Kb(),c.Lb(41,"ion-select",17),c.Tb("ngModelChange",function(e){return c.fc(n),c.Vb().registration.programId=e}),c.kc(42,g,2,2,"ion-select-option",18),c.Kb(),c.Kb(),c.Kb(),c.Lb(43,"ion-list"),c.Lb(44,"ion-button",19,20),c.Jb(46,"ion-icon",21),c.lc(47,"Speichern"),c.Kb(),c.Kb(),c.Kb(),c.Kb()}if(2&e){var i=c.ec(2),r=c.Vb();c.yb(9),c.bc("ngIf",0===r.registration.id&&r.clubAthletList.length>0),c.yb(4),c.bc("readonly",r.registration.athletId>0)("disabled",!1)("ngModel",r.registration.name),c.yb(1),c.bc("readonly",r.registration.athletId>0)("disabled",!1)("ngModel",r.registration.vorname),c.yb(6),c.bc("readonly",r.registration.athletId>0)("disabled",!1)("ngModel",r.registration.gebdat),c.yb(6),c.bc("disabled",r.registration.athletId>0)("ngModel",r.registration.geschlecht),c.yb(1),c.bc("value","W"),c.yb(2),c.bc("value","M"),c.yb(12),c.bc("ngModel",r.registration.programId),c.yb(1),c.bc("ngForOf",r.wkPgms),c.yb(2),c.bc("disabled",r.waiting||!r.isFormValid()||!i.valid||i.untouched)}}function h(e,t){if(1&e){var n=c.Mb();c.Lb(0,"ion-button",23,24),c.Tb("click",function(){return c.fc(n),c.Vb().delete()}),c.Jb(2,"ion-icon",25),c.lc(3,"Anmeldung l\xf6schen"),c.Kb()}if(2&e){var i=c.Vb();c.bc("disabled",i.waiting)}}var p=[{path:"",component:function(){function e(e,t,n,i,r){this.navCtrl=e,this.route=t,this.backendService=n,this.alertCtrl=i,this.zone=r,this.waiting=!1}return e.prototype.ngOnInit=function(){var e=this;this.waiting=!0,this.wkId=this.route.snapshot.paramMap.get("wkId"),this.regId=parseInt(this.route.snapshot.paramMap.get("regId")),this.athletId=parseInt(this.route.snapshot.paramMap.get("athletId")),this.backendService.getCompetitions().subscribe(function(t){var n=t.find(function(t){return t.uuid===e.wkId});e.wettkampfId=parseInt(n.id),e.backendService.loadProgramsForCompetition(n.uuid).subscribe(function(t){e.wkPgms=t,e.athletId?e.backendService.loadAthletRegistrations(e.wkId,e.regId).subscribe(function(t){e.updateUI(t.find(function(t){return t.id===e.athletId}))}):(e.backendService.loadAthletListForClub(e.wkId,e.regId).subscribe(function(t){e.clubAthletList=t}),e.updateUI({id:0,vereinregistrationId:e.regId,name:"",vorname:"",geschlecht:"W",gebdat:void 0,programId:void 0,registrationTime:0}))})})},Object.defineProperty(e.prototype,"selectedClubAthletId",{get:function(){return this._selectedClubAthletId},set:function(e){this._selectedClubAthletId=e,this.registration=this.clubAthletList.find(function(t){return t.athletId===e})},enumerable:!1,configurable:!0}),e.prototype.editable=function(){return this.backendService.loggedIn},e.prototype.updateUI=function(e){var t=this;this.zone.run(function(){t.waiting=!1,t.wettkampf=t.backendService.competitionName,t.registration=e,t.registration.gebdat=Object(l.d)(t.registration.gebdat)})},e.prototype.isFormValid=function(){return!!this.registration.gebdat&&!!this.registration.geschlecht&&this.registration.geschlecht.length>0&&!!this.registration.name&&this.registration.name.length>0&&!!this.registration.vorname&&this.registration.vorname.length>0&&!!this.registration.programId&&this.registration.programId>0},e.prototype.save=function(e){var t=this,n=Object.assign({},this.registration,{gebdat:new Date(e.gebdat).toJSON()});0===this.athletId||0===n.id?this.backendService.createAthletRegistration(this.wkId,this.regId,n).subscribe(function(){t.navCtrl.pop()}):this.backendService.saveAthletRegistration(this.wkId,this.regId,n).subscribe(function(){t.navCtrl.pop()})},e.prototype.delete=function(){var e=this;this.alertCtrl.create({header:"Achtung",subHeader:"L\xf6schen der Athlet-Anmeldung am Wettkampf",message:"Hiermit wird die Anmeldung von "+this.registration.name+", "+this.registration.vorname+" am Wettkampf gel\xf6scht.",buttons:[{text:"ABBRECHEN",role:"cancel",handler:function(){}},{text:"OKAY",handler:function(){e.backendService.deleteAthletRegistration(e.wkId,e.regId,e.registration).subscribe(function(){e.navCtrl.pop()})}}]}).then(function(e){return e.present()})},e.\u0275fac=function(t){return new(t||e)(c.Ib(o.N),c.Ib(a.a),c.Ib(b.a),c.Ib(o.b),c.Ib(c.A))},e.\u0275cmp=c.Cb({type:e,selectors:[["app-reg-athlet-editor"]],decls:14,vars:3,consts:[["slot","start"],["defaultHref","/"],["slot","end"],[1,"athlet"],[4,"ngIf"],["size","large","expand","block","color","danger",3,"disabled","click",4,"ngIf"],[3,"ngSubmit","keyup.enter"],["athletRegistrationForm","ngForm"],["src","assets/imgs/athlete.png"],["placeholder","Auswahl aus Liste","name","clubAthletid","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange",4,"ngIf"],["required","","placeholder","Name","type","text","name","name","required","",3,"readonly","disabled","ngModel","ngModelChange"],["required","","placeholder","Vorname","type","text","name","vorname","required","",3,"readonly","disabled","ngModel","ngModelChange"],["color","primary"],["required","","placeholder","Geburtsdatum","type","date","display-timezone","utc","name","gebdat","required","",3,"readonly","disabled","ngModel","ngModelChange"],["required","","placeholder","Geschlecht","name","geschlecht","okText","Okay","cancelText","Abbrechen","required","",3,"disabled","ngModel","ngModelChange"],[3,"value"],["src","assets/imgs/wettkampf.png"],["required","","placeholder","Programm/Kategorie","name","programId","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange"],[3,"value",4,"ngFor","ngForOf"],["size","large","expand","block","type","submit","color","success",3,"disabled"],["btnSaveNext",""],["slot","start","name",""],["placeholder","Auswahl aus Liste","name","clubAthletid","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange"],["size","large","expand","block","color","danger",3,"disabled","click"],["btnDelete",""],["slot","start","name","trash"]],template:function(e,t){1&e&&(c.Lb(0,"ion-header"),c.Lb(1,"ion-toolbar"),c.Lb(2,"ion-buttons",0),c.Jb(3,"ion-back-button",1),c.Kb(),c.Lb(4,"ion-title"),c.lc(5,"Athlet-Anmeldung"),c.Kb(),c.Lb(6,"ion-note",2),c.Lb(7,"div",3),c.lc(8),c.Kb(),c.Kb(),c.Kb(),c.Kb(),c.kc(9,u,48,17,"ion-content",4),c.Lb(10,"ion-footer"),c.Lb(11,"ion-toolbar"),c.Lb(12,"ion-list"),c.kc(13,h,4,1,"ion-button",5),c.Kb(),c.Kb(),c.Kb()),2&e&&(c.yb(8),c.nc("f\xfcr ",t.wettkampf,""),c.yb(1),c.bc("ngIf",t.registration),c.yb(4),c.bc("ngIf",t.athletId>0))},directives:[o.n,o.J,o.h,o.e,o.f,o.I,o.z,i.m,o.l,o.w,o.k,r.j,r.e,r.f,o.r,o.d,o.v,o.q,o.p,o.R,r.i,r.d,r.g,o.D,o.Q,o.E,i.l,o.g,o.o],pipes:[i.e],styles:[""]}),e}()}],m=function(){function e(){}return e.\u0275mod=c.Gb({type:e}),e.\u0275inj=c.Fb({factory:function(t){return new(t||e)},imports:[[i.c,r.a,o.K,a.i.forChild(p)]]}),e}()}}]);