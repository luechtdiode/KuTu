(window.webpackJsonp=window.webpackJsonp||[]).push([[16],{"l2/Q":function(t,e,n){"use strict";n.r(e),n.d(e,"RegAthletlistPageModule",function(){return O});var i=n("ofXK"),o=n("3Pt+"),r=n("tyNb"),c=n("TEn/"),s=n("cygB"),a=n("2Vo4"),b=n("XNiG"),u=n("LRne"),l=n("IzEk"),g=n("pLZG"),p=n("lJxs"),d=n("Kj3r"),f=n("/uUt"),h=n("vkgz"),m=n("eIep"),v=n("w1tV"),k=n("fXoL"),y=(n("ppBR"),function(){function t(){this.selected=new k.o}return t.prototype.ngOnInit=function(){},t.prototype.getProgrammText=function(t){var e=this.programmlist.find(function(e){return e.id===t});return e?e.name:"..."},t.\u0275fac=function(e){return new(e||t)},t.\u0275cmp=k.Bb({type:t,selectors:[["app-reg-athlet-item"]],inputs:{athletregistration:"athletregistration",status:"status",programmlist:"programmlist"},outputs:{selected:"selected"},decls:13,vars:8,consts:[[3,"click"],["slot","start"],["src","assets/imgs/athlete.png"],["slot","end"]],template:function(t,e){1&t&&(k.Kb(0,"ion-item",0),k.Sb("click",function(){return e.selected?e.selected.emit(e.athletregistration):{}}),k.Kb(1,"ion-avatar",1),k.Ib(2,"img",2),k.Jb(),k.Kb(3,"ion-label"),k.kc(4),k.Ib(5,"br"),k.Kb(6,"small"),k.kc(7),k.Vb(8,"date"),k.Jb(),k.Jb(),k.Kb(9,"ion-note",3),k.kc(10," Programm/Kategorie"),k.Ib(11,"br"),k.kc(12),k.Jb(),k.Jb()),2&t&&(k.xb(4),k.oc("",e.athletregistration.name,", ",e.athletregistration.vorname," (",e.athletregistration.geschlecht,")"),k.xb(3),k.mc("(",k.Wb(8,6,e.athletregistration.gebdat),")"),k.xb(5),k.nc("",e.getProgrammText(e.athletregistration.programId),", ",e.status," "))},directives:[c.q,c.d,c.v,c.z],pipes:[i.e],styles:[""]}),t}());function I(t,e){if(1&t&&(k.Kb(0,"ion-select-option",10),k.kc(1),k.Vb(2,"date"),k.Jb()),2&t){var n=e.$implicit;k.ac("value",n.uuid),k.xb(1),k.mc(" ",n.titel+" "+k.Xb(2,2,n.datum,"dd-MM-yy"),"")}}function S(t,e){if(1&t){var n=k.Lb();k.Kb(0,"ion-select",8),k.Sb("ngModelChange",function(t){return k.ec(n),k.Ub(2).competition=t}),k.jc(1,I,3,5,"ion-select-option",9),k.Jb()}if(2&t){var i=k.Ub(2);k.ac("ngModel",i.competition),k.xb(1),k.ac("ngForOf",i.getCompetitions())}}function x(t,e){if(1&t&&(k.Kb(0,"ion-col"),k.Kb(1,"ion-item"),k.Kb(2,"ion-label"),k.kc(3,"Wettkampf"),k.Jb(),k.jc(4,S,2,2,"ion-select",7),k.Jb(),k.Jb()),2&t){var n=k.Ub();k.xb(4),k.ac("ngIf",n.getCompetitions().length>0)}}function K(t,e){if(1&t&&(k.Kb(0,"ion-note",11),k.kc(1),k.Jb()),2&t){var n=k.Ub();k.xb(1),k.mc(" ",n.competitionName()," ")}}function J(t,e){1&t&&(k.Kb(0,"ion-item"),k.Kb(1,"ion-label"),k.kc(2,"loading ..."),k.Jb(),k.Ib(3,"ion-spinner"),k.Jb())}function A(t,e){if(1&t){var n=k.Lb();k.Kb(0,"ion-item-option",19),k.Sb("click",function(){k.ec(n);var t=k.Ub().$implicit,e=k.dc(1);return k.Ub(2).delete(t,e)}),k.Ib(1,"ion-icon",17),k.kc(2," L\xf6schen "),k.Jb()}}function C(t,e){if(1&t){var n=k.Lb();k.Kb(0,"ion-item-sliding",null,13),k.Kb(2,"app-reg-athlet-item",14),k.Sb("click",function(){k.ec(n);var t=e.$implicit,i=k.dc(1);return k.Ub(2).itemTapped(t,i)}),k.Jb(),k.Kb(3,"ion-item-options",15),k.Kb(4,"ion-item-option",16),k.Sb("click",function(){k.ec(n);var t=e.$implicit,i=k.dc(1);return k.Ub(2).edit(t,i)}),k.Ib(5,"ion-icon",17),k.kc(6),k.Jb(),k.jc(7,A,3,0,"ion-item-option",18),k.Jb(),k.Jb()}if(2&t){var i=e.$implicit,o=k.Ub(2);k.xb(2),k.ac("athletregistration",i)("status",o.getStatus(i))("programmlist",o.wkPgms),k.xb(4),k.mc(" ",o.isLoggedInAsClub()?"Bearbeiten":o.isLoggedIn()?"Best\xe4tigen":"Anzeigen"," "),k.xb(1),k.ac("ngIf",o.isLoggedInAsClub())}}function w(t,e){if(1&t&&(k.Kb(0,"ion-content"),k.Kb(1,"ion-list"),k.jc(2,J,4,0,"ion-item",3),k.Vb(3,"async"),k.jc(4,C,8,5,"ion-item-sliding",12),k.Jb(),k.Jb()),2&t){var n=k.Ub();k.xb(2),k.ac("ngIf",k.Wb(3,2,n.isBusy)||!(n.competition&&n.currentRegistration&&n.sAthletRegistrationList)),k.xb(2),k.ac("ngForOf",n.filteredStartList)}}function L(t,e){if(1&t){var n=k.Lb();k.Kb(0,"ion-button",20),k.Sb("click",function(){return k.ec(n),k.Ub().createRegistration()}),k.Ib(1,"ion-icon",21),k.kc(2," Neue Anmeldung... "),k.Jb()}}var R=[{path:"",component:function(){function t(t,e,n,i){this.navCtrl=t,this.route=e,this.backendService=n,this.alertCtrl=i,this.busy=new a.a(!1),this.tMyQueryStream=new b.a,this.sFilterTask=void 0,this.sSyncActions=[],this.backendService.competitions||this.backendService.getCompetitions()}return t.prototype.ngOnInit=function(){this.busy.next(!0);var t=this.route.snapshot.paramMap.get("wkId");this.currentRegId=parseInt(this.route.snapshot.paramMap.get("regId")),t?this.competition=t:this.backendService.competition&&(this.competition=this.backendService.competition)},t.prototype.ionViewWillEnter=function(){this.refreshList()},t.prototype.getSyncActions=function(){var t=this;this.backendService.loadRegistrationSyncActions().pipe(Object(l.a)(1)).subscribe(function(e){t.sSyncActions=e})},t.prototype.getStatus=function(t){return this.sSyncActions?this.sSyncActions.find(function(e){return e.verein.id===t.vereinregistrationId&&e.caption.indexOf(t.name)>-1&&e.caption.indexOf(t.vorname)>-1})?"pending":"in sync":"n/a"},t.prototype.refreshList=function(){var t=this;this.busy.next(!0),this.currentRegistration=void 0,this.getSyncActions(),this.backendService.getClubRegistrations(this.competition).pipe(Object(g.a)(function(e){return!!e.find(function(e){return e.id===t.currentRegId})}),Object(l.a)(1)).subscribe(function(e){t.currentRegistration=e.find(function(e){return e.id===t.currentRegId}),console.log("ask athletes-list for registration"),t.backendService.loadAthletRegistrations(t.competition,t.currentRegId).subscribe(function(e){t.busy.next(!1),t.athletregistrations=e,t.tMyQueryStream.pipe(Object(g.a)(function(t){return!!t&&!!t.target}),Object(p.a)(function(t){return t.target.value||"*"}),Object(d.a)(300),Object(f.a)(),Object(h.a)(function(e){return t.busy.next(!0)}),Object(m.a)(t.runQuery(e)),Object(v.a)()).subscribe(function(e){t.sFilteredRegistrationList=e,t.busy.next(!1)})})})},Object.defineProperty(t.prototype,"competition",{get:function(){return this.backendService.competition||""},set:function(t){var e=this;this.currentRegistration&&t===this.backendService.competition||this.backendService.loadProgramsForCompetition(this.competition).subscribe(function(t){e.wkPgms=t})},enumerable:!1,configurable:!0}),t.prototype.getCompetitions=function(){return this.backendService.competitions||[]},t.prototype.competitionName=function(){return this.backendService.competitionName},t.prototype.runQuery=function(t){var e=this;return function(n){var i=n.trim(),o=[];return i&&t&&t.forEach(function(t){e.filter(i)(t)&&o.push(t)}),Object(u.a)(o)}},Object.defineProperty(t.prototype,"athletregistrations",{get:function(){return this.sAthletRegistrationList},set:function(t){this.sAthletRegistrationList=t,this.reloadList(this.sMyQuery)},enumerable:!1,configurable:!0}),Object.defineProperty(t.prototype,"filteredStartList",{get:function(){return this.sFilteredRegistrationList||this.sAthletRegistrationList||[]},enumerable:!1,configurable:!0}),t.prototype.reloadList=function(t){this.tMyQueryStream.next(t)},t.prototype.itemTapped=function(t,e){e.getOpenAmount().then(function(t){t>0?e.close():e.open("end")})},t.prototype.isLoggedInAsClub=function(){return this.backendService.loggedIn&&this.backendService.authenticatedClubId===this.currentRegId+""},t.prototype.isLoggedInAsAdmin=function(){return this.backendService.loggedIn&&!!this.backendService.authenticatedClubId},t.prototype.filter=function(t){var e=this,n=t.toUpperCase().split(" ");return function(i){return"*"===t.trim()||n.filter(function(t){return i.name.toUpperCase().indexOf(t)>-1||i.vorname.toUpperCase().indexOf(t)>-1||!!e.wkPgms.find(function(e){return i.programId===e.id&&e.name===t})||i.gebdat.indexOf(t)>-1||void 0}).length===n.length}},t.prototype.createRegistration=function(){this.navCtrl.navigateForward("reg-athletlist/"+this.backendService.competition+"/"+this.currentRegId+"/0")},t.prototype.edit=function(t,e){this.navCtrl.navigateForward("reg-athletlist/"+this.backendService.competition+"/"+this.currentRegId+"/"+t.id),e.close()},t.prototype.delete=function(t,e){var n=this;e.close(),this.alertCtrl.create({header:"Achtung",subHeader:"L\xf6schen der Athlet-Anmeldung am Wettkampf",message:"Hiermit wird die Anmeldung von "+t.name+", "+t.vorname+" am Wettkampf gel\xf6scht.",buttons:[{text:"ABBRECHEN",role:"cancel",handler:function(){}},{text:"OKAY",handler:function(){n.backendService.deleteAthletRegistration(n.backendService.competition,n.currentRegId,t).subscribe(function(){n.refreshList()})}}]}).then(function(t){return t.present()})},t.\u0275fac=function(e){return new(e||t)(k.Hb(c.N),k.Hb(r.a),k.Hb(s.a),k.Hb(c.b))},t.\u0275cmp=k.Bb({type:t,selectors:[["app-reg-athletlist"]],decls:18,vars:5,consts:[["slot","start"],["defaultHref","/"],["no-padding",""],[4,"ngIf"],["slot","end",4,"ngIf"],["placeholder","Search","showCancelButton","never",3,"ngModel","ngModelChange","ionInput","ionCancel"],["size","large","expand","block","color","success",3,"click",4,"ngIf"],["placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange",4,"ngIf"],["placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange"],[3,"value",4,"ngFor","ngForOf"],[3,"value"],["slot","end"],[4,"ngFor","ngForOf"],["slidingAthletRegistrationItem",""],[3,"athletregistration","status","programmlist","click"],["side","end"],["color","primary",3,"click"],["name","arrow-dropright-circle","ios","md-arrow-dropright-circle"],["color","danger",3,"click",4,"ngIf"],["color","danger",3,"click"],["size","large","expand","block","color","success",3,"click"],["slot","start","name","add"]],template:function(t,e){1&t&&(k.Kb(0,"ion-header"),k.Kb(1,"ion-toolbar"),k.Kb(2,"ion-buttons",0),k.Ib(3,"ion-back-button",1),k.Jb(),k.Kb(4,"ion-title"),k.Kb(5,"ion-grid",2),k.Kb(6,"ion-row"),k.Kb(7,"ion-col"),k.Kb(8,"ion-label"),k.kc(9,"Wettkampf-Anmeldungen"),k.Jb(),k.Jb(),k.jc(10,x,5,1,"ion-col",3),k.Jb(),k.Jb(),k.Jb(),k.jc(11,K,2,1,"ion-note",4),k.Jb(),k.Kb(12,"ion-searchbar",5),k.Sb("ngModelChange",function(t){return e.sMyQuery=t})("ionInput",function(t){return e.reloadList(t)})("ionCancel",function(t){return e.reloadList(t)}),k.Jb(),k.Jb(),k.jc(13,w,5,4,"ion-content",3),k.Kb(14,"ion-footer"),k.Kb(15,"ion-toolbar"),k.Kb(16,"ion-list"),k.jc(17,L,3,0,"ion-button",6),k.Jb(),k.Jb(),k.Jb()),2&t&&(k.xb(10),k.ac("ngIf",!e.competition),k.xb(1),k.ac("ngIf",e.competition),k.xb(1),k.ac("ngModel",e.sMyQuery),k.xb(1),k.ac("ngIf",e.wkPgms&&e.wkPgms.length>0),k.xb(4),k.ac("ngIf",e.isLoggedInAsClub()))},directives:[c.n,c.J,c.h,c.e,c.f,c.I,c.m,c.B,c.j,c.v,i.m,c.C,c.R,o.d,o.g,c.l,c.w,c.q,c.D,c.Q,i.l,c.E,c.z,c.k,c.F,c.u,y,c.t,c.s,c.o,c.g],pipes:[i.e,i.b],styles:[""]}),t}()}],O=function(){function t(){}return t.\u0275mod=k.Fb({type:t}),t.\u0275inj=k.Eb({factory:function(e){return new(e||t)},imports:[[i.c,o.a,c.K,r.i.forChild(R)]]}),t}()}}]);