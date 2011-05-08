Name:           perl-Satcon
Summary:        Framework for configuration files
Version:        1.18
Release:        1%{?dist}
License:        GPLv2
Group:          Applications/System
URL:            https://fedorahosted.org/spacewalk
BuildRoot:      %{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id_u} -n)
BuildArch:      noarch
%if 0%{?suse_version}
Requires:      perl = %{perl_version}
%else
Requires:       perl(:MODULE_COMPAT_%(eval "`%{__perl} -V:version`"; echo $version))
%endif
Source0:        https://fedorahosted.org/releases/s/p/spacewalk/%{name}-%{version}.tar.gz
BuildRequires:  perl(ExtUtils::MakeMaker)
%if 0%{?suse_version}
Requires: policycoreutils
%else
Requires:       /sbin/restorecon
%endif

%description
Framework for generating config files during installation.
This package include Satcon perl module and supporting applications.

%prep
%setup -q

%build
%{__perl} Makefile.PL INSTALLDIRS=vendor
make %{?_smp_mflags}

%install
rm -rf $RPM_BUILD_ROOT

make pure_install PERL_INSTALL_ROOT=$RPM_BUILD_ROOT

find $RPM_BUILD_ROOT -type f -name .packlist -exec rm -f {} \;
find $RPM_BUILD_ROOT -depth -type d -exec rmdir {} 2>/dev/null \;

%{_fixperms} $RPM_BUILD_ROOT/*

%check
make test

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root,-)
%doc README LICENSE 
%{perl_vendorlib}/*
%{_bindir}/*

%changelog
* Tue May 03 2011 Jan Pazdziora 1.18-1
- Do chgrp apache for files being deployed.

* Thu Apr 28 2011 Jan Pazdziora 1.17-1
- Do not confuse me by saying Unsubstituted Tags when there are none.
- Do not deploy .orig files.
- When creating config files in /etc/rhn, clear access for other (make it
  -rw-r-----, in typical case).

* Mon Apr 25 2011 Jan Pazdziora 1.16-1
- The File::Copy and File::Temp do not seem to be used in Satcon, removing the
  use.

* Thu Apr 21 2011 Jan Pazdziora 1.15-1
- When creating the backup directory, do not leave them open for other, just
  owner and group should be enough.
- Use cp -p instead of File::Copy::copy to preserve the access rights.

* Fri Feb 18 2011 Jan Pazdziora 1.14-1
- Localize the filehandle globs; also use three-parameter opens.

* Tue Jan 11 2011 Jan Pazdziora 1.13-1
- Removing satcon-make-rpm.pl from repository as we haven't been packaging it
  since 2008.

* Tue Dec 14 2010 Jan Pazdziora 1.12-1
- We need to check the return value of GetOptions and die if the parameters
  were not correct.

* Wed Nov 25 2009 Miroslav Suchý <msuchy@redhat.com> 1.11-1
- 520441 - don't apply ExtUtils::MY->fixin(shift) to perl executables

* Tue Dec 16 2008 Miroslav Suchý <msuchy@redhat.com> 1.10-1
- remove satcon-make-rpm.pl

* Thu Nov 20 2008 Jan Pazdziora 1.9-1
- make satcon-deploy-tree.pl SELinux-aware

* Wed Oct 29 2008 Miroslav Suchý <msuchy@redhat.com> 1.8-1
- BZ 466777 - add link to tgz, add LICENSE file
* Thu Oct 23 2008 Miroslav Suchý <msuchy@redhat.com> 1.7-1
- remove explicit VERSION
* Mon Oct 13 2008 Miroslav Suchý <msuchy@redhat.com> 1.6-1
- edit comment for URL
* Mon Aug  4 2008 Jan Pazdziora 1.3-11
- rebuild
* Fri Apr 27 2007 Matthew Davis <mdavis@redhat.com> - 1.3-7
- Backup config files
* Tue Jul 20 2004 Robin Norwood <rnorwood@redhat.com> - 1.2
- New Satcon version - 1.2
* Wed May 05 2004 Chip Turner <cturner@redhat.com> - 1.1-2
- Specfile autogenerated.
